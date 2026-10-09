# Build decisions and open items

Unattended sessions cannot ask questions, so every choice that would normally be a question is already answered here. If a choice is not covered, take the simplest option that matches the spec, apply it, and add one line under "Decisions made during the build". Never stop to ask.

## Decisions already made
1. **Layout.** The backend stays in `prepai/` (it is not moved to `backend/`). The frontend goes in `frontend/` at the repo root. Row 1 updates the repo-layout sentence in spec 9.1 to match.
2. **Nightly verifier.** It lives in `generation/quality` and is built as row 9b. It calls the model named by `VERIFIER_MODEL` (Claude Fable 5.1) in production only; no test or build step calls it.
3. **Frontend stack.** Current stable Angular from `npx @angular/cli new`, standalone components, Angular Material, Konva used directly (no `ng2-konva` wrapper), KaTeX, Playwright for E2E with the system Chromium at `/usr/bin/chromium`. Use the test runner that `ng new` generates.
4. **Tests use local databases, not Docker.** Integration tests use PostgreSQL database `prepai_test` (user `prepai`, pgvector installed) and Redis database 15, through a `test` Spring profile. Testcontainers stays in `build.gradle` but is not used in this build (no Docker on the shared VPS for tests).
5. **Smoke test.** Row 2 replaces the generated `PrepaiApplicationTests` with a light context test on the `test` profile so `./gradlew check` does not need a real LLM key, a model download or the production database. If the local embedding model cannot load in tests, exclude its auto-configuration in the `test` profile.
6. **No real external calls, and no Anthropic API key exists.** The Claude Pro plan does not include API access, and no API key is available. Tests and the build never call Claude, VoiceStudio, Razorpay, SMTP or Google. Each sits behind a small interface with a fake. The application starts with the dummy key from the `local` profile, and no step may ask for a real key.
7. **Fake lesson provider.** Without a key the app uses a `FakeLessonProvider` (selected with `prepai.llm.provider=fake`, the default in the `local` and `test` profiles). It returns canned lessons built from the sample lesson in spec 3.2, passed through the real validator, so `lesson`, the whiteboard and the UI work end to end and can be demonstrated. The real `ClaudeProvider` is still written and unit-tested against a mocked client; it is simply never exercised against the live API.
8. **Gradle.** Run `nice -n 15 ./prepai/gradlew -p prepai -q <tasks>` from the repo root (memory limits are in `prepai/gradle.properties`). One Gradle process at a time.
9. **No long-running processes.** Do not leave a server, a Gradle daemon or a browser running after a step. Ports 8085, 9091 and 9095 stay free except during a short test.
10. **Spec edits.** The spec is read-only during the build except for the version line, the changelog and corrections found while building; log each correction below.
11. **Commits.** One per finished feature or bug fix, `git add` with explicit paths (never `git add -A`), messages `feat(<module>): ...`, `fix(<module>): ...`, `test(...)`, `docs(...)`, `chore(...)`. Commit locally and never push from the VPS until the user says "push" (local unattended runs never push at all; cloud sessions push only their own `row-<id>` branch, which is how they hand work over).
12. **Shared VPS.** No `sudo`, no access to MongoDB, QuestDB, Grafana or the trading services, nothing outside the repo directory. `creds.md` in the repo root is off limits and is git-ignored.
13. **Cloud sessions.** The repository lives at `github.com/ocjpkrishna/prepai` (private), and part of the build runs in cloud sessions on the included credit. Cloud sandboxes have no PostgreSQL, Redis or Docker, so any test that needs one is tagged `@Tag("db")` and skipped with `-PskipDbTests`; those tests run on the VPS (database `prepai_test`) before a row is accepted. Cloud sessions work on a `row-<id>` branch and open a pull request, never touch `BUILD-RUNBOOK.md`, and never push to `main`.

## Decisions made during the build
(One line each: date, row, decision, reason.)
- 2026-10-08, row 1: Angular is the current stable `ng new` (22.2), not 18: decision 3 chose it and spec 9.2 only asks for "18+". Test runner is the generated Vitest; no ESLint (not in the `ng new` default), so the frontend "lint" gate is open (see handoff).
- 2026-10-08, row 1: production database and role are `prepai_prod`, not `prepai`, because this VPS already has the development database `prepai` with role `prepai`.
- 2026-10-08, row 1: `scripts/install.sh` installs only missing packages and only verifies Java 21, Node 18+ and Redis, which are shared with the trading services. It never installs or upgrades them.
- 2026-10-08, row 1: the nginx site is rendered only after the Let's Encrypt certificate exists (`certbot certonly --webroot`), so the first run never fails on a missing certificate.
- 2026-10-08, row 1: the CI deploy job is off until the repository variable `DEPLOY_ENABLED` is `true`, because `scripts/deploy.sh` comes in row 12. Backend CI runs with `-PskipDbTests`.
- 2026-10-08, row 2: `Language` enum added (spec 3.1 lists `en`/`hi`; the languages are in spec 1.6, not in the four shared enums of 9.1); JSON names fixed with `@JsonProperty`.
- 2026-10-08, row 2: `CanvasAction.config` is a `Map<String, Object>` and the action type an enum; per-type config checks belong to the validator (row 7), because 3.3 defines keys per type.
- 2026-10-08, row 2: plan limits are constants inside `Plan` (magic-number gate); the Plan tiers' numbers are the spec 1.4 pricing figures.
- 2026-10-08, row 2: `ApiException` is the single base class; domain exceptions are added by owning modules, not subclassed here. 401 and 403 are two classes (`UnauthenticatedEntryPoint`, `ForbiddenAccessDeniedHandler`) sharing `ErrorResponseWriter`.
- 2026-10-08, row 2: the catch-all is `@ExceptionHandler(Exception.class)`, not a catch block (checkstyle IllegalCatch forbids catch blocks). Spring's 405/415 are not mapped yet.
- 2026-10-08, row 2: the context test runs on a `test` profile that excludes the database, Redis and embedding auto-configurations (decision 5), so it needs no infrastructure and runs in cloud sessions too.
- 2026-10-08, row 2: `Supplier<UUID>` and `Clock` beans live in `common/config`; the trace-ID filter uses the former.
- 2026-10-08, row 1: spec 9.1 repo-layout sentence corrected to `prepai/` plus `frontend/` (decision 1); `ARCHITECTURE.md` frontend row updated to match.
- 2026-10-08, row 3: built on the session model (Haiku 5.5), not the ★ Sonnet tier the table names. Gates are green; a Sonnet `/code-review` of `account/auth` belongs in row 20.
- 2026-10-08, row 3: email verification (spec 4.1, V8) is in row 3, because without it every new account stays blocked by `EMAIL_NOT_VERIFIED`. SMTP sits behind `VerificationMailer`; no provider yet (Needs the user).
- 2026-10-08, row 3: timestamps in V1, V7 and V8 are `TIMESTAMP WITH TIME ZONE`, not the spec 5.1 `TIMESTAMP`, so `Instant` maps under `ddl-auto: validate` without conversion. Spec 5.1 correction candidate.
- 2026-10-08, row 3: `users.language` stores the enum name (`EN`, `HI`); the JSON names stay `en` and `hi`. Its default is `EN`.
- 2026-10-08, row 3: an invalid or expired verification link is `VALIDATION_FAILED` (400); spec 4.7 has no code for it.
- 2026-10-08, row 3: Google sign-in joins an existing account only if its email is verified; otherwise `EMAIL_ALREADY_EXISTS`, which blocks account takeover through a pre-created unverified password account.
- 2026-10-08, row 3: sign-up takes email, password and name only. Terms, the age gate and guardian consent wait for row 4 (`privacy`).
- 2026-10-08, row 3: the signup throttle (`signup:{ip}:{hour}` in Redis) is not built yet. It needs Redis in the request path, so it comes with the Redis rate limiter in row 5.
- 2026-10-08, row 3: the `test` profile has no JPA, so `PrepaiApplicationTests` and `AuthSecurityTest` replace the repositories and the mailer with `@MockitoBean`s (`org.springframework.test.context.bean.override.mockito`).
- 2026-10-08, row 4: the age gate takes a birth date at sign-up, which only sets `is_minor` and is never stored (spec 2.7 stores the age flag). The guardian link lasts 7 days (the spec gives no lifetime).
- 2026-10-08, row 4: the policy version is the constant `ConsentService.POLICY_VERSION` ("2026-10"), since the spec names no versioning scheme; the counsel review (TODO-1) decides the real one.
- 2026-10-08, row 4: `POST /api/v1/auth/guardian-consent/confirm?token=` answers 204, and an invalid or expired link is `VALIDATION_FAILED`, as in row 3.
- 2026-10-08, row 4: preferences add `voice_speed` (0.5 to 2.0) and `theme` (`light` or `dark`) as V9 columns. Spec 4.3 gives the fields but no ranges. V9 also adds `purged_at`.
- 2026-10-08, row 4: `DELETE /users/me` answers 204, revokes every refresh token at once, and blocks login (password and Google) until the purge. The purge runs nightly at 02:15 and anonymises the `users` row instead of deleting it, so ids in billing rows still resolve.
- 2026-10-08, row 4: lessons and usage are not built yet. They join the export and the purge through `PersonalDataContributor` (`common/model`), which rows 6, 13 and 14 implement. `GET /users/me/usage` waits for row 6, which owns `usage_log`.
- 2026-10-08, row 4: `ConsentService` and `GuardianConsentService` are split so that `ConsentService` stays under the fan-out limit (15).
- 2026-10-08, row 5: built on the session model (Haiku 5.5), not the ★ Sonnet tier the table names (same as row 3). Sonnet `/code-review` of `quota/ratelimit` and the signup throttle belongs in row 20.
- 2026-10-08, row 5: burst limit is 5 lesson attempts per minute (`BurstLimiter`); the spec gives no number. Extract limit is 20 per hour from spec 4.2.
- 2026-10-08, row 5: the student's day for the daily limit starts at midnight Asia/Kolkata; the spec does not say which zone. `DAILY_LIMIT_REACHED` carries `retryAfterSeconds` to that midnight.
- 2026-10-08, row 5: each Redis counter is one Lua script (`INCR` plus `EXPIRE` on creation, `DECR` only if present), so a counter never lacks a TTL. `RedisCounter` sits in `common/redis` because `quota` and `account` both use it.
- 2026-10-08, row 5: the signup throttle is a `HandlerInterceptor` on `POST /api/v1/auth/register`, not controller code, because the controller would exceed the fan-out limit (15). It counts before validation.
- 2026-10-08, row 5: `SIGNUPS_PER_IP_PER_HOUR` binds to `prepai.privacy.signups-per-ip-per-hour` (the key `application.yaml` already had, with no class bound to it) through `SignupProperties`.
- 2026-10-09, row 6: `usage_log.lesson_id` has no foreign key until row 13 creates `lessons`, because V3 runs before that table exists; row 13 adds the constraint.
- 2026-10-09, row 6: `sessionsToday` and the summary count `usage_log` rows (delivered sessions), not the Redis reservations, which only gate the limit. Both use the same Asia/Kolkata day.
- 2026-10-09, row 6: the context tests (`PrepaiApplicationTests`, `AuthSecurityTest`) mock `UsageLogRepository` with `@MockitoBean`, like the other repositories, because the `test` profile has no JPA.
- 2026-10-09, row 7: the parse layer is `ParseRule`, not a `ValidationRule`, because it reads text before a lesson exists. An unknown action type fails the parse (the enum), so the canvas layer checks only `null` types.
- 2026-10-09, row 7: HTML is stripped (jsoup, `Safelist.none()`) by `LessonSanitizer` before the rules run, not reported as an error, because spec 2.6 says "stripped" and a retry would cost an LLM call.
- 2026-10-09, row 7: spec 3.3 "labels" for `DRAW_AXIS` means `xLabel` and `yLabel`, as in the spec 3.2 sample. "Drawing" actions are `DRAW_*` and `HIGHLIGHT_REGION` (x 0 to 500); `WRITE_TEXT` and `WRITE_LATEX` may use the whole 800 x 500 canvas.
- 2026-10-09, row 7: the markup character check (`\ _ ^ $ { }`) covers step and summary narration; the 600-character limit covers step narration only; the LaTeX denylist covers equation `latex` and `WRITE_LATEX` config. Spec 2.6 names no limit for the summary.
- 2026-10-09, row 8: built on the session model (Haiku 5.5), not the ★ Sonnet tier the table names (same as rows 3 and 5). Sonnet `/code-review` of `generation/llm` belongs in row 20.
- 2026-10-09, row 8: the Claude client is Spring AI `ChatModel` (already in `build.gradle`), so the `spring.ai.anthropic.*` settings stay the source of truth. Prompt caching marks the system prompt with Spring AI's `SYSTEM_ONLY` cache strategy. Both compile; neither is exercised live.
- 2026-10-09, row 8: `LlmProvider` is named that way, not `LLMProvider`, to match the other names in the package. The Ollama `LocalLLMProvider` is not built: its starter is not in `build.gradle` and `FakeLessonProvider` covers development.
- 2026-10-09, row 8: validation errors go to `PromptTemplateService.repair` as a parameter, not into `LessonRequest` (spec 2.6 names `LessonRequest.validationErrors`). The request stays the student's input only.
- 2026-10-09, row 8: `LessonGenerationService` maps the final provider failure to `LLM_UNAVAILABLE` (503) and a final invalid answer to `LESSON_GENERATION_FAILED` (502). An open breaker fails at once with `LLM_UNAVAILABLE` and makes no call.
- 2026-10-09, row 8: the retry pause (`prepai.llm.retry-backoff`, 1 s by default, 0 in tests) is a plain `LockSupport.parkNanos`. Spec 2.6 asks to honour `Retry-After`; this build does not read it yet.
- 2026-10-09, row 8: the fake provider returns the spec 3.2 sample from `resources/llm/fake-lesson.json` for every question (decision 7).
- 2026-10-09, row 9: built on the session model (Haiku 5.5), not the ★ Sonnet tier the table names (same as rows 3, 5 and 8). Sonnet `/code-review` of `generation/rag` and `cache` belongs in row 20.
- 2026-10-09, row 9: the RAG cache stores the normalised text (lowercase, canonical units after a number, redactions) as `problem_text`, because matching and the verifier both work on that form.
- 2026-10-09, row 9: only Indian mobile numbers are redacted (`[6-9]` plus nine digits, optional +91 and 5+5 spacing), so a 10-digit physics quantity keeps its number in the signature. Emails are redacted too.
- 2026-10-09, row 9: a numeric signature longer than 500 characters (the column size, spec 5.1) is not cached at all. Cutting it short could let two different problems match.
- 2026-10-09, row 9: the signature keeps signs and power signs (`-3`, `10^-11`), so `10^11` and `10^-11` never match. Any extra character makes a miss, which is the safe direction.
- 2026-10-09, row 9: the exact-match key is SHA-256 of the request's JSON (spec 5.3 "identical inputs"), so no two different requests can share a key.
- 2026-10-09, row 9: V5 timestamps are `TIMESTAMP WITH TIME ZONE` (same as V1, V7 and V8). `created_at` is set by the database default, so the entity never writes it.
- 2026-10-09, row 9: `subject` and `exam` are stored as enum names in `String` fields, which keeps the entity free of enum types.
- 2026-10-09, row 9: `RagService` does not call Redis or the LLM by itself; `lesson` (row 13) calls the cache and RAG before and after generation. The `prepai_lesson_generated_total` metric also comes with row 13.
- 2026-10-09, row 9: the `prepai_rag_unverified_backlog` gauge is deferred to row 9b, which owns the verifier and the rows it counts.
- 2026-10-09, row 9: the db tests run on the VPS against `prepai_test` (5 pgvector and mapping tests) and Redis database 15 (3 cache tests); both passed in this run.
- 2026-10-09, row 9b: built on the session model (Haiku 5.5), as the table names. Sonnet `/code-review` of `generation/quality` belongs in row 20 (quality is not a ★ module, so it is optional there).
- 2026-10-09, row 9b: V6 has no foreign key on `quality_corrections.lesson_id` (V2 `lessons` comes in row 13), same as `usage_log` in row 6. `lesson_id` is not mapped, because the verifier never has a lesson id.
- 2026-10-09, row 9b: the `db` tests for V6 and the V5 queries use `spring.flyway.out-of-order=true`, because the shared `prepai_test` already has V7 to V9 applied from earlier rows, so V6 arrives out of order and Flyway refuses it. Production and dev databases are created fresh, so they apply V1 to V9 in order and do not need it.
- 2026-10-09, row 9b: the batch takes unverified rows only, most asked first (spec 8.1 step 1). The random question-bank fill is not built, because the repo has no question bank.
- 2026-10-09, row 9b: a corrected lesson is generated with `LessonGenerationService` (Sonnet) and stored as verified without a second check by the verifier, as spec 8.1 step 5d says. Fable grades only the original.
- 2026-10-09, row 9b: a correction is regenerated with `Type.PROBLEM`, `Difficulty.MEDIUM` and `Language.EN`, because the stored rows keep neither difficulty nor language.
- 2026-10-09, row 9b: the verifier does not evict `lesson:cache` keys (spec 8.1 step 5d). The key hashes the student's original text, which the row does not keep. The 7-day TTL bounds staleness. See "Needs the user".
- 2026-10-09, row 9b: spec 8.1 step 5c (updating few-shot examples on a pattern) is not automated. `quality_corrections` is the record a person reads to decide on a prompt change.
- 2026-10-09, row 9b: spec 8.1 step 7 (nightly report) is one log line per batch: verified, corrected and failed counts. Retry and cache-hit rates are not in it.
- 2026-10-09, row 9b: the verifier prompt is ours (spec 6 has none): `prompts/verifier-system-prompt.txt`, read by `VerifierSystemPrompt`. It asks for grade JSON only. Any other reply counts as a failed call.
- 2026-10-09, row 9b: the verifier runs only under the `prod` profile (`NightlyVerifierJob`, `VerificationService`, `CorrectionService`, `ClaudeVerifierClient`). It is scheduled at 02:30, after the 02:15 purge. Tests use a mocked `VerifierClient`.
- 2026-10-09, row 9b: `ClaudeFailures` (in `llm/service`) holds the Spring AI error mapping, shared by `ClaudeProvider` and `ClaudeVerifierClient`. The behaviour of `ClaudeProvider` is unchanged.
- 2026-10-09, row 9b: spec 5.2 names the index on `quality_corrections(error_type)` `idx_quality_corrections_subject`. The build uses `idx_quality_corrections_error_type`, which matches its column. Spec correction candidate.
- 2026-10-09, row 9b: the `prepai_rag_unverified_backlog` gauge is named `prepai.rag.unverified.backlog` (exported with underscores), as the spec 8.4 table asks.
- 2026-10-09, row 10: built on the session model (Haiku 5.5), as the table names. Sonnet `/code-review` of `generation/imageextract` is optional in row 20 (not a ★ module).
- 2026-10-09, row 10: `LlmPrompt` gets an optional photo (`LlmImage`); `purpose()` is `IMAGE_EXTRACT` when a photo is attached, else `LESSON`. `LlmCaller` and `LlmMetrics` take the purpose from the prompt, so extraction shares the `claude` breaker and the spec 8.4 metrics.
- 2026-10-09, row 10: extraction is one call with no retry, because spec 3.1.1 names none. A breaker open or a provider failure is `LLM_UNAVAILABLE` (503).
- 2026-10-09, row 10: a photo that is too large is `IMAGE_TOO_LARGE` (413), and one without a JPEG, PNG or WebP signature is `IMAGE_UNSUPPORTED` (415). A file whose structure is broken despite the right signature is also `IMAGE_UNSUPPORTED`, so the student sees "use a JPEG, PNG or WebP photo" and no model call is made.
- 2026-10-09, row 10: LOW confidence, an empty `problemText` or a reply that is not the JSON object is `IMAGE_UNREADABLE` (422). Spec 3.1.1 names no other rule for an unreadable reply.
- 2026-10-09, row 10: metadata stripping keeps JPEG APP0 (JFIF), APP2 (colour profile) and APP14 (colour flags) and drops other APPn segments and comments; PNG drops eXIf and text chunks and drops anything after IEND; WebP drops EXIF and XMP chunks and rewrites the RIFF size. Spec 2.7 requires EXIF and GPS removal only, so nothing else is changed.
- 2026-10-09, row 10: the spec gives no extraction prompt, so `prompts/image-extract-system-prompt.txt` is ours (JSON only; text in the photo is data). The fake provider answers photo prompts with `llm/fake-extraction.json` (decision 7).
- 2026-10-09, row 10: the 1600 px downscale is the frontend's job (spec 3.1.1); the server does not resize. The extract rate limit (`ratelimit:extract`) is applied by `lesson` in row 13, not here.
- 2026-10-09, row 15: built in a cloud session on Sonnet 5.5. No razorpay-java dependency: `RazorpayGateway` has only `FakeRazorpayGateway` (decision 6). The webhook signature check is real (HMAC-SHA256, constant-time compare) and needs no SDK.
- 2026-10-09, row 15: idempotency uses the `X-Razorpay-Event-Id` header, stored in `processed_webhook_events` (V4) with the event's effect in one transaction. A missing id or bad signature is `VALIDATION_FAILED` (400); an event for an unknown subscription is recorded and ignored.
- 2026-10-09, row 15: plan ids in the API are the lower-case enum names (`free`, `pro`, `pro_plus`); checkout accepts only the paid ones and `paymentMethod` `razorpay`. Prices (199, 399) are constants in `PlanCatalogService` (spec 1.4).
- 2026-10-09, row 15: `GET /subscriptions/me` reports the plan the student really has (from `account`), with the latest subscription's status (`NONE`, `CREATED`, `ACTIVE`, `CANCELLED`) and period end. An unpaid checkout never changes the plan.
- 2026-10-09, row 15: `User.planExpiresAt` was missing from the entity (the V1 column existed); added, plus `UserService.changePlan`. A missing `current_end` in an event defaults to start + 30 days. V4 timestamps are WITH TIME ZONE and `status` defaults to `CREATED`.
- 2026-10-09, row 15: the lapse of `plan_expires_at` without a Razorpay event (and `quota` honouring it) is not built; the webhook is the only way a plan ends.

## Needs the user
Collected here so nobody has to be interrupted. Review after the build.

| Item | Why it is needed |
|------|------------------|
| Anthropic API key (a separate pay-per-use account; the Claude Pro plan does not provide one) | **Not available.** Needed before the product can generate real lessons; until then the fake provider is used. Also needed for one live smoke test of lesson generation and image extraction, and to measure real cost per lesson (spec 15, TODO-4). That smoke test must also confirm that `ClaudeProvider` maps Spring AI's HTTP and timeout errors to the right retry reasons, and that the system-prompt cache option takes effect (row 8) |
| Razorpay test keys and webhook secret | End-to-end payment flow (spec 15, TODO-3) |
| Google OAuth client id | Real Google sign-in |
| Domain and SMTP provider | Verification and guardian-consent emails (TODO-7) |
| Grafana service-account token and alert route | `scripts/grafana-sync.sh` (TODO-5) |
| Production database, role and secrets file on the VPS | `install.sh` generates them; the user supplies the real secrets |
| Legal review and provider data terms | Privacy design and processors (TODO-1, TODO-2) |
| Cost validation | Real per-lesson cost against the target (TODO-4) |
| Embedding quality check | Threshold tuning on real paraphrases (TODO-6) |
| Cache seed run | After the verifier exists (TODO-8) |
| Angular version check | Confirm the version `ng new` picked suits the team |
| VoiceStudio source and install | `install.sh` only reports it; the TTS server's source is not in this repository (spec 10.1) |
| CI secrets and deploy switch | `VPS_HOST`, `VPS_USER`, `VPS_SSH_KEY`, `VPS_KNOWN_HOSTS` secrets and the `DEPLOY_ENABLED` variable, set after row 12 |
| Production secrets file | `install.sh` generates `DB_PASSWORD` and `JWT_SECRET` in `/etc/prepai/prepai.env`; the operator fills the rest (Google, Claude, Razorpay, SMTP) |
| PostgreSQL check of the auth schema | V1, V7 and V8 and the `ddl-auto: validate` mapping have only been checked by reading. Run them on the VPS against `prepai_test` with a `db`-tagged test before row 4 relies on them |
| PostgreSQL check of V9 and the privacy queries | V9 (`voice_speed`, `theme`, `purged_at`), the purge query and the revocation queries have only been checked by reading. A `db`-tagged test on `prepai_test` is needed (row 4 scope) |
| Google sign-up has no age gate or terms step | The Google request carries neither a birth date nor terms, so Google accounts skip both (`terms_accepted_at` stays empty). The frontend and the API must send them before launch (spec 2.7) |
| PostgreSQL check of V3 (`usage_log`) | V3 and the `UsageLog` mapping under `ddl-auto: validate` have only been checked by reading. A `db`-tagged test on `prepai_test` is needed (row 6 scope) |
| Privacy policy version | `ConsentService.POLICY_VERSION` is a placeholder ("2026-10") until counsel approves the policy (TODO-1) |
| Verifier cannot evict exact-match cache keys | Spec 8.1 step 5d deletes the `lesson:cache` key of a corrected lesson, but that key hashes the student's original text, and `problem_embeddings` keeps only the normalised text. Choose: store the request hash on the row (a new column in a later migration, row 13 or 9b rework) or accept the 7-day TTL. Until then an identical request can get the old lesson for up to 7 days |
| Verifier live check | No Anthropic key, so `ClaudeVerifierClient` (model `VERIFIER_MODEL`, the prompt-cache option, the grade JSON reply and the 429 and timeout mapping) is only unit-tested against a mocked `ChatModel`. One live call is needed, as with row 8 |
| Question bank for the verifier batch | Spec 8.1 step 1 fills the batch from a question bank, and TODO-8 seeds the cache with `QUALITY_SEED_SIZE` problems. No question bank exists in the repo, so the batch takes unverified stored rows only |
| `prepai_test` migration history | `prepai_test` has V7 to V9 applied, so V6 runs only because the db tests set `out-of-order`. Optional: recreate `prepai_test` (a test-only database) so that its history is V1 to V9 in order, and drop the flag |
| PostgreSQL check of V4 (`subscriptions`, `processed_webhook_events`) | V4 and the entity mappings under `ddl-auto: validate` have only been checked by reading; a `db`-tagged test on `prepai_test` is needed (row 15 scope). Razorpay's real payload and the event-id header need a check with test keys. |
