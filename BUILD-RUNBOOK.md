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
| 1 | Agent 1, rest | `frontend/` scaffold (Angular, Material, Konva, KaTeX), `scripts/install.sh`, CI workflow, `.env.example`, nginx template, `MODULE.md` stubs for frontend and ops | Haiku | [x] |
| 2 | common | errors, shared enums, lesson contract records, config beans | Haiku | [x] |
| 3 | account/auth | register, login, refresh rotation, Google sign-in, security config | ★ Sonnet | [x] |
| 4 | account/user + privacy | profile, export, deletion, consent, purge jobs | Haiku | [x] |
| 5 | quota/ratelimit | reserve, commit, release (atomic Redis), burst and extract limits | ★ Sonnet | [x] |
| 6 | quota/usage | usage log and summary endpoint | Haiku | [x] |
| 7 | generation/validation | `LessonValidator` and the rule classes | Haiku | [x] |
| 8 | generation/llm | Claude provider, prompts, retry and repair, circuit breaker, metrics | ★ Sonnet | [x] |
| 9 | generation/embedding + rag + cache | local embeddings, verified-only lookup, numeric signature, Redis cache | ★ Sonnet | [x] |
| 9b | generation/quality | nightly verifier and corrections job (calls Fable 5.1 in production only), `verified` promotion | Haiku | [x] |
| 10 | generation/imageextract | image sanitising and extraction | Haiku | [x] |
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
- 2026-10-08, row 1 done (built locally by a subagent on Haiku, verified by the maintainer): Angular app in `frontend/` (Material, Konva, KaTeX, Playwright; production build and `ng test` pass), `scripts/install.sh` (idempotent, not yet run), nginx template, `.env.example`, CI workflow (deploy off until row 12), MODULE.md stubs for the frontend features and `ops/`. Open: frontend lint (ESLint) is not configured; VoiceStudio has no source in the repo. Next: row 2.
- 2026-10-08, row 2 done (unattended, Haiku): `common` errors (ErrorCode, ApiException, ApiError envelope, GlobalExceptionHandler, 401/403 handlers, TraceIdFilter), enums (Plan, Subject, Exam, Difficulty, Language), lesson contract records (3.1, 3.2), Clock and ID config beans, light `test` profile and context test. `./gradlew check` green (27 tests). Open: `/simplify` run inline (no agents); `/code-review` not run. Next: row 3 (★ Sonnet) or row 4/7 per the cloud order.
- 2026-10-08, row 3 done (unattended, built on Haiku 5.5, not the ★ Sonnet tier): `account/auth` with register (plus email verification), login, refresh rotation with reuse detection, Google sign-in, JWT and security config, V1/V7/V8 migrations, unit tests and a MockMvc security test. `./gradlew check` green. Open: signup throttle (row 5, Redis); SQL and `ddl-auto: validate` not yet checked against PostgreSQL (`db` test on the VPS); no `/code-review` run; `/simplify` done inline. Next: row 4 (Haiku), then row 5 (★ Sonnet).
- 2026-10-08, row 4 done (unattended, Haiku 5.5): `account/user` (profile, preferences, export, deletion request) and `account/privacy` (sign-up age gate and terms, guardian consent link, `AccountGateService`, soft deletion, nightly 30-day purge), V9 migration, `PersonalDataContributor` in `common`. Unit tests for every service; `./gradlew check` green. Open: no `db` test for V9 and the purge; no HTTP test for the user endpoints; `/code-review` and a separate `/simplify` pass not run; Google sign-up skips the age gate and terms (BUILD-DECISIONS.md). Next: row 5 (★ Sonnet while plan usage allows); its `db` tests run on the VPS (Redis).
- 2026-10-08, row 5 done (unattended, built on Haiku 5.5, not the ★ Sonnet tier): `quota/ratelimit` (reserve and commit or release with `SessionReservation`, daily limit per plan, burst 5/min, extract 20/hour, all as atomic Redis scripts in `common/redis/RedisCounter`), plus the signup throttle (`SignupThrottle` behind `SignupThrottleInterceptor`, 5 per IP per hour). `./gradlew check` green with the `db` tests run against local Redis (`RedisCounterRedisTest`, 2 pass). Open: no `/code-review` (Sonnet, row 20); `/simplify` done inline; the throttle's client address is only right behind nginx with forwarded headers (prod profile, row 12 checks it). Next: row 6 (Haiku), which owns `usage_log` (V3) and the usage section of the export.
- 2026-10-09, row 6 done (unattended, Haiku 5.5): `quota/usage` (`UsageService.recordSession` for lesson, `GET /users/me/usage`, `UsageDataContributor` for export and purge), V3 `usage_log`, unit tests; the two context tests now mock `UsageLogRepository`. `./gradlew check` green, db tests included. Open: no `db` test for V3 (`lesson_id` foreign key comes with row 13); no `/code-review` (row 20); `/simplify` done inline. Next: row 7 (Haiku).
- 2026-10-09, row 7 done (unattended, Haiku 5.5): `generation/validation` (`ParseRule`, `StructureRule`, `CanvasRule`, `CanvasBoundsRule`, `TextSafetyRule`, `LessonSanitizer`, `LessonValidator`, result and error models), tests on a valid spec 3.2 lesson, one break per test. `./gradlew check` green. Open: no `/code-review` (row 20); `/simplify` done inline; `LessonValidator` has no caller until row 8. Next: row 8 (generation/llm).
- 2026-10-09, row 8 done (unattended, Haiku 5.5, not the ★ Sonnet tier): `generation/llm` with `ClaudeProvider` on Spring AI (`ClaudeTranslator`), `FakeLessonProvider` (spec 3.2 sample, selected in `local` and `test`), `PromptTemplateService` (spec 6.1 and 6.2 as resources), `LlmCaller` behind the resilience4j `claude` breaker, `GenerationRetryService` (transient retry and repair, two calls at most), the `LessonGenerationService` facade and `LlmMetrics` (spec 8.4). Unit tests use a scripted provider and a mocked `ChatModel`; `./gradlew check` green. Open: no live Claude call (no key), so the Spring AI error mapping and the prompt-cache option are unverified (BUILD-DECISIONS "Needs the user"); Ollama provider not built; no `/code-review` (row 20); `/simplify` done inline. Next: row 9 (embedding, rag and cache; it owns V5 and its `db` tests run on the VPS).
- 2026-10-09, row 9 done (unattended, Haiku 5.5, not the ★ Sonnet tier): `generation/embedding` (`EmbeddingService`), `generation/rag` (`RagService` lookup and store, `ProblemProbeService`, `TextNormalizer` with redaction, `NumericSignatureService`, `ProblemEmbeddingRepository` with the pgvector query, `RagMetrics`), `generation/cache` (`LessonCacheService`, Redis `LessonCacheRepository`), V5 migration. `./gradlew check` green, db tests included (5 pgvector and mapping tests on `prepai_test`, 3 Redis tests). Open: no `/code-review` (row 20); `/simplify` done inline; `bootRun` not started, so the transformers model load in `local` is unverified; `lesson` wires the cache and RAG (row 13); V6 and the verifier are row 9b. Next: row 9b (Haiku).
- 2026-10-09, row 9b done (unattended, Haiku 5.5): `generation/quality` (`VerificationService` batch, `CorrectionService` with V6 `quality_corrections`, `NightlyVerifierJob` at 02:30, `QualityMetrics` backlog gauge), `ClaudeVerifierClient` (Fable 5.1, `prod` only, via shared `ClaudeFailures`), `ProblemReviewService` in `rag`, V6 migration. `./gradlew check` green, db tests included. Open: no live Fable call (no key); the verifier does not evict `lesson:cache` keys (see "Needs the user"); the `prepai_test` history needs out-of-order for V6; no `/code-review` (row 20). Next: row 10 (generation/imageextract, Haiku).
- 2026-10-09, row 10 done (unattended, Haiku 5.5): `generation/imageextract` (`ImageExtractionService`, `ImageSanitizer` with JPEG, PNG and WebP metadata strippers, `ExtractionReplyParser`, `ExtractionPrompt`, `ImageProperties`), and `LlmPrompt` now carries an optional photo (`LlmImage`, `LlmPurpose` derived from it) through `ClaudeTranslator`, `LlmCaller` and `LlmMetrics`. `./gradlew check` green. Open: no live vision call (no key); the Claude media request is only unit-tested with a mocked `ChatModel`; no `/code-review` (row 20); `/simplify` done inline. Next: row 11 (Agent 5 whiteboard engine, ★ Sonnet tier, frontend).
