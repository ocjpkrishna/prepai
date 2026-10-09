# common

**Purpose:** the shared foundation every other module may use: the one error format of the API, shared configuration beans, and the few models that more than one module needs. It contains no business logic and depends on no business module.
**Built by:** Agent 1 (`config`), Agent 2 (`errors`), Agent 3 (the lesson contract in `model/lesson`) | **Spec:** 3.1, 3.2, 4.7, 9.1.1

## Context
- Every endpoint of every module returns errors in the shape of spec 4.7, so the error model lives here instead of inside any one module.
- A class belongs here only when two or more modules need it. Everything else stays in its own module.

## Packages
```
common/
├── MODULE.md
├── errors/    ErrorCode (status + default message), ApiException (the one exception modules throw),
│              ApiError and ApiErrorResponse (the JSON envelope), GlobalExceptionHandler (@RestControllerAdvice),
│              ErrorResponseWriter (writes an error to a raw response), UnauthenticatedEntryPoint (401),
│              ForbiddenAccessDeniedHandler (403), TraceIdFilter (MDC and X-Trace-Id header)
├── config/    ClockConfig (one UTC Clock), IdConfig (Supplier<UUID> for new IDs),
│              AppProperties (prepai.app.*: base URL, CORS origins, sender address)
├── redis/     RedisCounter (atomic increment with expiry, decrement if present; Lua scripts),
│              HourWindow (fixed one-hour windows and seconds left in them)
└── model/
    ├── enums/    Plan (FREE, PRO, PRO_PLUS with their limits), Subject, Exam, Difficulty, Language (en, hi)
    └── lesson/   LessonRequest (with Type and Input), LessonResponse (steps, canvas actions, equations,
                  summary, mastery check; spec 3.1, 3.2)
```

## Other modules may call
Everything here. Modules throw `ApiException` with an `ErrorCode` instead of writing their own exception handlers. Domain codes are added to `ErrorCode`; domain exceptions reuse `ApiException` rather than adding subclasses.

## Data
None. No tables. `RedisCounter` takes any key the caller names; the key names belong to the modules that use it (`quota`, `account`).

## Rules and gotchas
- `common` must never import from a business module (the ArchUnit test checks it).
- No repositories and no entities here.
- The `lesson` contract classes are records. Agent 3 owns their definition, Agent 4 and the Angular models depend on it.
- `CanvasAction.config` is a map because its keys depend on the action type (spec 3.3); the validator (generation/validation) checks the keys per type.
- Unknown canvas action types fail JSON parsing, so the LLM output is repaired or retried rather than passed on.
- `GlobalExceptionHandler` catches `Exception` through `@ExceptionHandler`; it logs the detail and returns `INTERNAL_ERROR`. Spring's own 405 and 415 errors are not mapped yet and currently come back as `INTERNAL_ERROR`; fix when a module needs them.
- Errors raised outside the controllers (401, 403) go through `ErrorResponseWriter`, so both shapes match.
- The `test` profile (`src/test/resources/application-test.yml`) excludes the database, Redis and embedding-model auto-configurations, so the context test runs anywhere. Tests that need PostgreSQL or Redis are tagged `db`.

## Status
- [x] `errors`: ErrorCode, ApiError, handlers, TraceIdFilter
- [x] `model/enums`: Plan, Subject, Exam, Difficulty, Language
- [x] `model/lesson`: lesson contract
- [x] `config`: Clock and ID supplier
- [x] `redis`: RedisCounter and HourWindow (row 5; the `db` test `RedisCounterRedisTest` runs on the VPS)
