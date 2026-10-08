# PrepAI build runbook

Goal: build PrepAI in one continuous run at low cost, and make it safe to change model or open a fresh session at any moment without losing work. The progress table below is the single source of truth for where the build stands.

## How to work a row
1. Read `CLAUDE.md`, then the first open row of the progress table.
2. Read `prepai/ARCHITECTURE.md` and the `MODULE.md` of the module. Load spec sections with `scripts/spec-section.sh <number>` (for example `6.3`, or `agent3` for a task card). Never load the whole spec (about 37k tokens; a section is about 1k).
3. Write the code, then reach a **checkpoint**:
   - `./gradlew check` is green (frontend: lint, unit tests and a production build);
   - the module's `MODULE.md` is true (package map, status ticked);
   - the row below is ticked and one line is added to the handoff log;
   - a commit (see below).
4. Report in 10 lines or fewer.

## Commits: feature by feature, bug by bug
The user approved this on 2026-10-08.
- One commit per finished feature (a row, or a coherent part of a large row), each leaving `./gradlew check` green.
- Message style: `feat(<module>): <what it does>`, `fix(<module>): <what was wrong>`, `test(...)`, `docs(...)`, `chore(...)`; a short body only when the reason is not obvious.
- A bug gets a failing test first, then the fix, in one `fix(...)` commit.
- Never push, and never rewrite earlier commits, without asking.

## Model tiers
| Model | Price per million tokens (input / output) | Use for |
|-------|-------------------------------------------|---------|
| Claude Haiku 5.5 | $0.10 / $0.50 (prompts up to 100K tokens) | Default for the whole build: DTOs, mappers, controllers, simple services, tests, config, scripts, UI pages |
| Claude Sonnet 5.5 | $2 / $10 | Rows marked ★ only: security, money, concurrency and core algorithms, where a subtle bug costs the most |

Haiku is 20 times cheaper than Sonnet on both input and output. No more expensive model is used during the build. The quality gates (Checkstyle, ArchUnit, tests, coverage) catch weak output from either model.

If a Haiku attempt still fails `./gradlew check` after two fix rounds, do not keep going: write the failing gate and class in the handoff log, mark the row `needs Sonnet`, and continue with the next row.

## The 60% rule (switching to the cheaper model)
Claude cannot see the plan usage or change its own model; the user does both. When plan usage reaches about 60% (check `/usage` or `/cost`, whichever the installed version shows):
1. Finish the open checkpoint. Do not start a new row on Sonnet.
2. The user switches with `/model` in the session, or opens a new session with `claude --model claude-haiku-5-5`.
3. Every remaining row now runs on Haiku, including the ★ rows.
4. If budget remains at the end, run a Sonnet review (`/code-review`) on the ★ modules only, and fix what it finds.

## Progress
★ = Sonnet while budget allows (before the 60% mark), Haiku afterwards. Done column: `[ ]` open, `[x]` done, `[!]` blocked (the driver stops and explains why).

| # | Row | Area | Model | Done |
|---|-----|------|-------|------|
| 1 | Agent 1, rest | `frontend/` scaffold (Angular, Material, Konva, KaTeX), `scripts/install.sh`, CI workflow, `.env.example`, nginx template, `MODULE.md` stubs for frontend and ops | Haiku | [ ] |
| 2 | common | errors, shared enums, lesson contract records, config beans | Haiku | [ ] |
| 3 | account/auth | register, login, refresh rotation, Google sign-in, security config | ★ Sonnet | [ ] |
| 4 | account/user + privacy | profile, export, deletion, consent, purge jobs | Haiku | [ ] |
| 5 | quota/ratelimit | reserve, commit, release (atomic Redis), burst and extract limits | ★ Sonnet | [ ] |
| 6 | quota/usage | usage log and summary endpoint | Haiku | [ ] |
| 7 | generation/validation | `LessonValidator` and the rule classes | Haiku | [ ] |
| 8 | generation/llm | Claude provider, prompts, retry and repair, circuit breaker, metrics | ★ Sonnet | [ ] |
| 9 | generation/embedding + rag + cache | local embeddings, verified-only lookup, numeric signature, Redis cache | ★ Sonnet | [ ] |
| 9b | generation/quality | nightly verifier and corrections job (calls Fable 5.1 in production only), `verified` promotion | Haiku | [ ] |
| 10 | generation/imageextract | image sanitising and extraction | Haiku | [ ] |
| 11 | Agent 5 | whiteboard engine (Konva, KaTeX, animation) | ★ Sonnet | [ ] |
| 12 | Agent 9 | DevOps: Prometheus, Grafana sync, nginx, systemd, deploy script | Haiku | [ ] |
| 13 | lesson/orchestration | `LessonService`, generate and extract endpoints | ★ Sonnet | [ ] |
| 14 | lesson/history + feedback + mastery | remaining lesson endpoints | Haiku | [ ] |
| 15 | billing | plans, checkout, Razorpay webhook (signature, idempotency) | ★ Sonnet | [ ] |
| 16 | speech | TTS endpoints, audio cache, warm-up, cleanup | Haiku | [ ] |
| 17 | Agent 6 | TTS and voice sync, lesson player | ★ Sonnet | [ ] |
| 18 | Agent 7 | UI pages: landing, auth, dashboard, lesson input, pricing, profile | Haiku | [ ] |
| 19 | Agent 8 | integration, adversarial and performance tests, E2E | Haiku | [ ] |
| 20 | Review | Sonnet `/code-review` of the ★ modules, then fixes | Sonnet, if budget remains | [ ] |

Flyway migrations belong to the row that owns the table (spec 5.2).

## Unattended mode (no one answers questions)
`scripts/autobuild.sh` runs the whole table without anyone present: one fresh, small Claude session per row, each on the model the table names, until every row is `[x]`, a row is blocked, the budget is used, or a stop is requested.

```
BUDGET_USD=40 scripts/autobuild.sh              run it (set the total budget)
DRY_RUN=1 BUDGET_USD=40 scripts/autobuild.sh    show what would run, call nothing
touch .autobuild/STOP                           stop cleanly after the current row
```

- **The 60% rule is automatic.** The driver adds up the cost Claude reports for each session. Once the total reaches 60% of `BUDGET_USD` it runs every remaining row on Haiku. The figure is a list-price estimate, so treat the budget as a stand-in for your plan's usage limit.
- **Hard caps.** `ROW_BUDGET_USD` (default 4) limits one session, `BUDGET_USD` limits the whole run, `ROW_TIMEOUT` (default 90m) limits time, and only one run can be active at a time.
- **Failures.** A row that does not reach its checkpoint is tried once more (on Sonnet when budget allows). If it fails again, the row is marked `[!]` and the run stops with the reason in `.autobuild/STOPPED`.
- **No questions.** Sessions follow `BUILD-DECISIONS.md`, log new choices there, and list anything only a person can supply under "Needs the user" instead of stopping.
- **Guardrails.** Sessions run with `scripts/autobuild-settings.json` and `--permission-mode dontAsk`: anything not allowed is refused, not asked. They can read and write only inside this repository; `sudo`, `git push`, `rm -r`, `curl`, `systemctl`, anything under `/etc`, `/opt`, `/root` and `/var`, `.git`, `creds.md` and `.env` files, and the driver itself are blocked.
- **Between rows** the working tree must be clean (everything committed) or the driver stops.

## Resume prompt for a fresh session
Paste this, nothing more:

> Resume the PrepAI build. Read `CLAUDE.md` and `BUILD-RUNBOOK.md`, then continue with the first open row. Report in 10 lines or fewer.

## Handoff log
One line per checkpoint: date, row, what is done, what is next, any problem.

- 2026-10-08, setup: spec v2.9, module structure and `MODULE.md` files, quality gates (Checkstyle, ArchUnit, JaCoCo), build and profiles, PostgreSQL 17 and Redis 8 installed. No business code yet. Next: row 1.
