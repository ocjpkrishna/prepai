# Testing and integration

**Built by:** Agent 8 | **Spec:** 8.2, 8.4, 9.1.1, 9.1.2, 9.2 (Agent 8)

## Purpose
Prove the whole product works, and keep trying to break it. Each module's own unit tests live next to its package, mirroring the production path: `src/test/java/com/ascorp/prepai/<module>/<feature>/<layer>/`. The packages listed here hold only the tests that cross modules.

## Tools and targets
- JUnit 5, Mockito and AssertJ; Testcontainers for PostgreSQL (with pgvector) and Redis. Browser tests use Playwright or Cypress in `frontend/e2e/`.
- Targets: lesson generation under 10 s, canvas animation at 60 fps, page load under 3 s.
- The adversarial suite starts with 50 edge cases and grows: every bug it finds becomes a permanent test (spec 8.2).

## Packages
```
com/ascorp/prepai/
├── TESTING.md
├── architecture/     ArchitectureTest (ArchUnit rules of spec 9.1.1), ModuleDocumentationTest
│                     (every module has a MODULE.md). Add new modules to the lists as they appear.
├── integration/      full API flows with Testcontainers: register to rate, rate limit and upgrade prompt,
│                     mastery check, privacy (consent, export, deletion), the error contract for every
│                     code in spec 4.7
├── adversarial/      the 50+ edge cases: injection, malformed input, image attacks, rapid-fire requests
├── performance/      generation latency and load checks
└── support/          shared helpers: containers, fixtures, a fake Claude provider
```
Tests of validator rules, retry behaviour, RAG, TTS caching, privacy and observability sit in the module they exercise (`generation`, `speech`, `account`, ...).

## Rules
- Tests never call the real Claude API; use the fake provider. A small, clearly marked manual suite may call it.
- Tests use their own containers and never connect to the shared MongoDB, QuestDB or the production database.
- Failure injection: with VoiceStudio stopped, the lesson still plays with captions; with the provider failing, no quota is consumed.
- A found bug gets a test first; the fix goes back to the owning module.
- Test code follows spec 9.1.2: short Arrange-Act-Assert tests, one behaviour per test, named for the behaviour.

## Definition of done
All tests pass and CI is green. No P0 bugs in the critical flows: register, log in, generate, play, rate, hit the rate limit, upgrade, mastery check.

## Status
- [x] Architecture rules (`ArchitectureTest`)
- [ ] `ModuleDocumentationTest`
- [ ] Integration tests (Testcontainers)
- [ ] Error contract tests for every code in 4.7
- [ ] Adversarial suite (first 50 cases)
- [ ] Performance checks
- [ ] Frontend unit tests and E2E (`frontend/e2e/`)
