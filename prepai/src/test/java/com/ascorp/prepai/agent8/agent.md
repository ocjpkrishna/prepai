# Agent 8 - Testing and Integration

**Model:** Claude Sonnet 5.5 | **Depends on:** all other agents | **Spec:** `prepai-spec.md` sections 8.2, 8.4, 9.1.1, 9.2 (Agent 8)

## Purpose
Agent 8 proves the whole product works and keeps trying to break it. Each feature agent writes unit tests for its own code; Agent 8 owns everything that crosses agents: full API flows, failure injection, adversarial inputs, performance checks and the browser end-to-end tests.

## Context
- Backend tests run on JUnit 5 and Mockito, with Testcontainers for PostgreSQL (with pgvector) and Redis. Browser tests use Playwright or Cypress (`frontend/e2e/`).
- Each agent's own tests live next to its package: `src/test/java/com/ascorp/prepai/agentN/<feature>/<layer>/`. This folder only holds cross-cutting tests.
- The adversarial suite starts with 50 edge cases and grows: every bug it finds becomes a permanent test (spec 8.2).
- Targets: lesson generation under 10 s, canvas animation at 60 fps, page load under 3 s.

## Package map
```
agent8/                 (in src/test/java/com/ascorp/prepai/)
├── agent.md
├── integration/        full API flows with Testcontainers: register to rate, rate limit and upgrade prompt,
│                       mastery check, privacy (consent, export, deletion), error contract for every code in 4.7
├── llm/                validation fixtures (one per failure layer), retry and circuit-breaker tests with a
│                       mocked provider, RAG tests (numeric variants, unverified rows never served),
│                       20 known problems checked against the lesson JSON schema
├── adversarial/        the 50+ edge cases: injection, malformed input, image attacks, rapid-fire requests
├── tts/                audio cache, single-flight, cleanup job, VoiceStudio down
├── observability/      /actuator/prometheus exposes the 8.4 metrics and is not reachable through nginx
├── performance/        generation latency and load checks
└── support/            shared test helpers (containers, fixtures, a fake Claude provider)
```
Frontend end-to-end tests live in `frontend/e2e/`.

## Rules specific to this agent
- Tests never call the real Claude API; use the fake provider. A small, clearly marked manual suite may call it.
- Tests use their own containers and never connect to the shared MongoDB, QuestDB or the production database.
- Failure injection: with VoiceStudio stopped the lesson still plays with captions; with the provider failing, no quota is consumed.
- A found bug gets a test first, then the fix goes back to the owning agent.

## Definition of done
All tests pass and CI is green. No P0 bugs in the critical flows: register, log in, generate, play, rate, hit the rate limit, upgrade, mastery check.

## Status
- [ ] Backend integration tests (Testcontainers)
- [ ] Validator, retry/fallback, RAG and image tests
- [ ] Error contract tests for every code in 4.7
- [ ] TTS and privacy tests
- [ ] Observability tests
- [ ] Adversarial suite (first 50 cases)
- [ ] Performance checks
- [ ] Frontend unit tests and E2E (`frontend/e2e/`)
