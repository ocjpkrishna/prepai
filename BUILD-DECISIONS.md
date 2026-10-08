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
11. **Commits.** One per finished feature or bug fix, `git add` with explicit paths (never `git add -A`), messages `feat(<module>): ...`, `fix(<module>): ...`, `test(...)`, `docs(...)`, `chore(...)`. Never push to `main` (local unattended runs never push at all; cloud sessions push only their own `row-<id>` branch).
12. **Shared VPS.** No `sudo`, no access to MongoDB, QuestDB, Grafana or the trading services, nothing outside the repo directory. `creds.md` in the repo root is off limits and is git-ignored.
13. **Cloud sessions.** The repository lives at `github.com/ocjpkrishna/prepai` (private), and part of the build runs in cloud sessions on the included credit. Cloud sandboxes have no PostgreSQL, Redis or Docker, so any test that needs one is tagged `@Tag("db")` and skipped with `-PskipDbTests`; those tests run on the VPS (database `prepai_test`) before a row is accepted. Cloud sessions work on a `row-<id>` branch and open a pull request, never touch `BUILD-RUNBOOK.md`, and never push to `main`.

## Decisions made during the build
(One line each: date, row, decision, reason.)

## Needs the user
Collected here so nobody has to be interrupted. Review after the build.

| Item | Why it is needed |
|------|------------------|
| Anthropic API key (a separate pay-per-use account; the Claude Pro plan does not provide one) | **Not available.** Needed before the product can generate real lessons; until then the fake provider is used. Also needed for one live smoke test of lesson generation and image extraction, and to measure real cost per lesson (spec 15, TODO-4) |
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
