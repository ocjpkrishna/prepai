# Agent 1 - Project Scaffolder

**Model:** Claude Sonnet 5.5 | **Depends on:** nothing, runs first | **Spec:** `prepai-spec.md` sections 2.2, 2.3, 2.5, 9.1.1, 9.2 (Agent 1), 10

## Purpose
Agent 1 builds the skeleton that every other agent works inside, then steps aside. It owns the build, the configuration files, the repository layout and the first-time server setup. It writes very little Java: the application class and a few shared configuration beans.

## Context
- The backend is a Spring Boot 4.1.1 / Java 21 / Gradle project (group `com.ascorp`, root package `com.ascorp.prepai`), generated with Spring Initializr and then extended.
- Other agents only start once the build, the profiles (`application.yaml`, `application-local.yml`, `application-prod.yml`) and the folder rules in 9.1.1 exist.
- PostgreSQL 17 + pgvector and Redis 8 are already installed on the VPS. PostgreSQL has a `prepai` dev database. MongoDB and QuestDB on this VPS belong to other services and are never used by PrepAI.

## Package map
```
agent1/
├── agent.md
└── config/        shared configuration that belongs to no single feature
                   (for example a Clock bean for testable time, common Micrometer tags,
                   RestClient defaults). Created only when something needs it.
```
`PrepaiApplication` stays in the root package `com.ascorp.prepai`.

## Deliverables outside this package
| Deliverable | Location |
|-------------|----------|
| Gradle build and wrapper | `prepai/build.gradle`, `prepai/gradlew` |
| Spring profiles | `prepai/src/main/resources/application*.y*ml` |
| Angular 18 scaffold (port 4300) | `frontend/` |
| VPS setup script (idempotent, skips what is installed) | `scripts/install.sh` |
| CI: build, test, deploy | `.github/workflows/` |
| Environment template | `.env.example` |
| Nginx template | `nginx/` |
| `agent.md` for Agents 5, 6, 7 and 9 | see the table in spec 9.1.1 |

## Public surface
None. Other agents use what Agent 1 sets up, not its classes.

## Data owned and configuration
No tables or Redis keys. Defines the structure of every configuration property in `application.yaml` (`prepai.*`) and the matching environment variables in spec 10.3.

## Rules specific to this agent
- Never overwrite the existing Spring Initializr project; extend it.
- Production secrets have no defaults (spec 10.4).
- Do not use sudo, and never touch the shared services on the VPS (MongoDB, QuestDB, Grafana, trading services). Check ports with `lsof` before binding.
- `install.sh` must be safe to run twice.

## Definition of done
`./scripts/install.sh` sets up the VPS, `./gradlew bootRun` starts the backend on :8085 with the `local` profile, `ng serve --port 4300` starts the frontend, and `http://localhost:9091/actuator/prometheus` returns metrics.

## Status
- [x] Spring Boot 4.1.1 project generated, `build.gradle` reconciled with the spec
- [x] `application.yaml`, `application-local.yml`, `application-prod.yml`
- [x] PostgreSQL 17 + pgvector and Redis 8 installed on the VPS
- [ ] Angular 18 scaffold in `frontend/` (port 4300)
- [ ] `scripts/install.sh`
- [ ] GitHub Actions CI
- [ ] `.env.example`
- [ ] Nginx template
- [ ] `agent.md` files for Agents 5, 6, 7 and 9
- [ ] Confirm the monorepo layout (project currently at `prepai/`, spec says `backend/`)
