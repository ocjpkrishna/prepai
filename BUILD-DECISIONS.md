# Build decisions and open items

Unattended sessions cannot ask questions, so every choice that would normally be a question is already answered here. If a choice is not covered, take the simplest option that matches the spec, apply it, and add one line under "Decisions made during the build". Never stop to ask.

## Decisions already made
1. **Layout.** The backend stays in `prepai/` (it is not moved to `backend/`). The frontend goes in `frontend/` at the repo root. Row 1 updates the repo-layout sentence in spec 9.1 to match.
2. **Nightly verifier.** It lives in `generation/quality` and is built as row 9b. It calls the model named by `VERIFIER_MODEL` (Claude Fable 5.1) in production only; no test or build step calls it.
3. **Frontend stack.** Current stable Angular from `npx @angular/cli new`, standalone components, Angular Material, Konva used directly (no `ng2-konva` wrapper), KaTeX, Playwright for E2E with the system Chromium at `/usr/bin/chromium`. Use the test runner that `ng new` generates.
4. **Tests use local databases, not Docker.** Integration tests use PostgreSQL database `prepai_test` (user `prepai`, pgvector installed) and Redis database 15, through a `test` Spring profile. Testcontainers stays in `build.gradle` but is not used in this build (no Docker on the shared VPS for tests).
5. **Smoke test.** Row 2 replaces the generated `PrepaiApplicationTests` with a light context test on the `test` profile so `./gradlew check` does not need a real LLM key, a model download or the production database. If the local embedding model cannot load in tests, exclude its auto-configuration in the `test` profile.
6. **No real external calls.** Tests and the build never call Claude, VoiceStudio, Razorpay, SMTP or Google. Each sits behind a small interface with a fake. The application starts with the dummy key from the `local` profile.
7. **Gradle.** Run `nice -n 15 ./prepai/gradlew -p prepai -q <tasks>` from the repo root (memory limits are in `prepai/gradle.properties`). One Gradle process at a time.
8. **No long-running processes.** Do not leave a server, a Gradle daemon or a browser running after a step. Ports 8085, 9091 and 9095 stay free except during a short test.
9. **Spec edits.** The spec is read-only during the build except for the version line, the changelog and corrections found while building; log each correction below.
10. **Commits.** One per finished feature or bug fix, `git add` with explicit paths (never `git add -A`), messages `feat(<module>): ...`, `fix(<module>): ...`, `test(...)`, `docs(...)`, `chore(...)`. Never push.
11. **Shared VPS.** No `sudo`, no access to MongoDB, QuestDB, Grafana or the trading services, nothing outside the repo directory. `creds.md` in the repo root is off limits and is git-ignored.

## Decisions made during the build
(One line each: date, row, decision, reason.)

## Needs the user
Collected here so nobody has to be interrupted. Review after the build.

| Item | Why it is needed |
|------|------------------|
| Real `CLAUDE_API_KEY` | One live smoke test of Sonnet 5.5 lesson generation and image extraction |
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
