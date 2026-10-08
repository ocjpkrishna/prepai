# PrepAI

AI whiteboard tutor for Indian students (JEE, NEET, boards). Spring Boot 4.1 / Java 21 / Gradle backend in `prepai/`, Angular frontend in `frontend/` (not created yet). The product spec is `prepai-spec.md`: it is long, so never load it whole.

## Read in this order
0. `BUILD-RUNBOOK.md`: the progress table says which row is next and which model tier it uses.
1. `prepai/ARCHITECTURE.md`: the module map and the rules.
2. The `MODULE.md` of the module you are changing.
3. Only the spec sections that `MODULE.md` links to, loaded with `scripts/spec-section.sh <number>` (about 1k tokens each).

## Structure (spec 9.1.1)
- Code is organised by business module, never by agent: `prepai/src/main/java/com/ascorp/prepai/<module>/<feature>/{controller,service,repository,model,mapper}`. Create only the layers a feature needs.
- Controller → Service → Repository. Modules call each other's services only. Entities and repositories stay private to their module. Anything shared goes in `common`.
- Keep the module's `MODULE.md` true: package map, status checklist, gotchas.

## Code quality (spec 9.1.2)
- Code reads like a short story: the public method is a list of named steps, the details sit below it.
- Methods at most 20 lines and 4 parameters. SOLID. Constructor injection. Thin controllers. Tabs for Java.
- No `@Data`, no field injection, no `TODO` comments, no magic numbers, no catch-all `catch (Exception)`.
- `./gradlew check` (Checkstyle, ArchUnit, tests, coverage) must be green before a task is done. Never loosen or disable a gate; change the spec first.
- Run `/simplify` and `/code-review` on your diff before calling a task finished.

## Commands (run from the repo root, no `cd` needed)
- `nice -n 15 ./prepai/gradlew -p prepai -q check`: all quality gates.
- `nice -n 15 ./prepai/gradlew -p prepai -q test --tests '*SomeTest'`: one test class.
- `nice -n 15 ./prepai/gradlew -p prepai bootRun`: starts the app with the `local` profile on port 8085 (needs PostgreSQL 17 + pgvector and Redis 8, both installed on this VPS; stop it again when done).
- The VPS is shared: always `nice` and `-q`; memory limits are in `prepai/gradle.properties`; one Gradle process at a time.
- Unattended runs (`scripts/autobuild.sh`): nobody answers questions. Follow `BUILD-DECISIONS.md`, log new choices there, put anything only a person can supply under "Needs the user", and never stop to ask.

## Working rules (keep token use low)
- Report results in 10 lines or fewer. Do not re-print files or paste long logs.
- Read file ranges, not whole files. Edit existing files; do not rewrite them. Do not re-read a file after editing it.
- One module per session. Do not start extra parallel sessions unless the work is independent.
- Give tools exact targets (paths, test names) instead of searching broadly.
- At each checkpoint tick the progress row in `BUILD-RUNBOOK.md` and add one line to its handoff log, so a fresh session or a cheaper model can resume without re-reading anything.

## Shared VPS: hard limits
- PostgreSQL 17 and Redis 8 belong to PrepAI. MongoDB, QuestDB, Grafana and the trading services belong to other projects: read-only at most, never modify, restart or write to them (see also the user-level rules).
- Never use `sudo` without asking. Check a port with `lsof` before binding it. PrepAI uses 8085 (app), 9091 (management), 9095 (Prometheus).
- Never write credentials into files, specs or commits. Secrets come from the environment only.
- Commit after each finished feature or bug fix, one logical change per commit, with a message like `feat(<module>): ...` or `fix(<module>): ...` (the user approved this on 2026-10-08). A bug gets a failing test first, then the fix. Never push, and never rewrite earlier commits, without asking.
