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
In an interactive session Claude cannot see the plan usage or change its own model; the user does both. (`scripts/autobuild.sh` does this automatically from the plan usage Claude reports.) When plan usage reaches about 60% (check `/usage`, whichever the installed version shows):
1. Finish the open checkpoint. Do not start a new row on Sonnet.
2. The user switches with `/model` in the session, or opens a new session with `claude --model claude-haiku-5-5`.
3. Every remaining row now runs on Haiku, including the ★ rows.
4. If plan usage allows at the end, run a Sonnet review (`/code-review`) on the ★ modules only, and fix what it finds.

## Progress
★ = Sonnet while plan usage allows (before the 60% mark), Haiku afterwards. Done column: `[ ]` open, `[x]` done, `[!]` blocked (the driver stops and explains why).

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
| 20 | Review | Sonnet `/code-review` of the ★ modules, then fixes | Sonnet, if plan usage allows | [ ] |

Flyway migrations belong to the row that owns the table (spec 5.2).

## Unattended mode (no one answers questions)
`scripts/autobuild.sh` runs the whole table without anyone present: one fresh, small Claude session per row, each on the model the table names, until every row is `[x]`, a row is blocked, or a stop is requested. It uses only what the Claude plan (Pro) allows; there is no dollar budget and it never uses paid overage.

```
scripts/autobuild.sh                run it (best inside tmux)
DRY_RUN=1 scripts/autobuild.sh      show plan usage and the row plan, change nothing
touch .autobuild/STOP               stop cleanly after the current step
```

- **Real plan usage.** Claude reports the plan's usage windows (5-hour and weekly) in every headless session, and the driver reads them. At 60% of either window every remaining row runs on Haiku (`SWITCH_FRACTION`). At 90% it waits for that window to reset (`PAUSE_FRACTION`), checks every minute for a stop request, then continues on its own. If the plan ever reports paid overage, the run stops instead of spending money.
- **Plan limit reached mid-row.** The row is not counted as a failure: the driver waits for the reset and retries the same row.
- **Caps per session.** `ROW_TIMEOUT` (default 90m) and `ROW_BUDGET_USD` (default 6, a list-price runaway guard rather than money) limit one session. Only one run can be active at a time.
- **Failures.** A row that does not reach its checkpoint is tried once more (on Sonnet when plan usage allows). If it fails again, the row is marked `[!]` and the run stops with the reason in `.autobuild/STOPPED`.
- **No questions.** Sessions follow `BUILD-DECISIONS.md`, log new choices there, and list anything only a person can supply under "Needs the user" instead of stopping.
- **Guardrails.** Sessions run with `scripts/autobuild-settings.json` and `--permission-mode dontAsk`: anything not allowed is refused, not asked. They can read and write only inside this repository; `sudo`, `git push`, `rm -r`, `curl`, `systemctl`, anything under `/etc`, `/opt`, `/root` and `/var`, `.git`, `creds.md` and `.env` files, and the driver itself are blocked. No MCP servers or claude.ai connectors (Gmail, Drive, Hostinger and the rest) are loaded, which also saves about 5k prompt tokens and several helper processes per session.
- **Between rows** the working tree must be clean (everything committed) or the driver stops.
- **Shared allowance.** Your own use of Claude draws from the same plan windows. Weekly usage was already 80% on 2026-10-08, with the weekly reset at 2026-10-12 05:00 UTC.

## Cloud mode (uses the included cloud credit)
The plan includes $100 of cloud-session credit, valid until 2026-11-05 13:29 IST. It applies automatically to cloud sessions only (not to local sessions or `scripts/autobuild.sh`); afterwards the plan's usage applies, and there is no purchased credit to overspend.

- **Launch one row:** `claude --cloud "<prompt>" --model <haiku|sonnet>`, with the prompt from `scripts/cloud-prompt.sh <row id>`. Use the row's tier from the table.
- **One pull request per row.** The session works on branch `row-<id>`. The maintainer reviews, runs the `db` tests on the VPS, merges, and ticks the row here.
- **Order.** Rows 1 and 2 first, alone. Then these can run in parallel: 3, 7, 11, 12, 16. After that: 4 and 5 (need 3), 8, 9, 9b and 10 (need 7 or `common`), then 6, 13, 14, 15, 17, 18, 19.
- **Spend the credit on the ★ rows first** (3, 5, 8, 9, 11, 13, 15, 17), then the rest, then the deep review of row 20 with `/code-review ultra`, which also draws on the cloud credit.
- **Watch the balance** on the usage page. Stop launching new sessions when under $10 is left.

## Resume prompt for a fresh session
Paste this, nothing more:

> Resume the PrepAI build. Read `CLAUDE.md` and `BUILD-RUNBOOK.md`, then continue with the first open row. Report in 10 lines or fewer.

## Handoff log
One line per checkpoint: date, row, what is done, what is next, any problem.

- 2026-10-08, setup: spec v2.9, module structure and `MODULE.md` files, quality gates (Checkstyle, ArchUnit, JaCoCo), build and profiles, PostgreSQL 17 and Redis 8 installed. No business code yet. Next: row 1.
