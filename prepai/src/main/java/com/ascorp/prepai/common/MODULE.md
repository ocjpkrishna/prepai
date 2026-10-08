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
├── errors/    ErrorCode, ApiError, ApiException and subclasses, GlobalExceptionHandler,
│              SecurityErrorHandlers (401/403 in the same shape), TraceIdFilter
├── config/    shared configuration beans (a Clock for testable time, common metric tags,
│              RestClient defaults), created only when something needs them
└── model/
    ├── enums/    Plan (FREE, PRO, PRO_PLUS and their limits), Subject, Exam, Difficulty
    └── lesson/   LessonRequest, LessonResponse and their nested classes (spec 3.1, 3.2)
```

## Other modules may call
Everything here. Modules throw `ApiException` with an `ErrorCode` instead of writing their own exception handlers.

## Data
None. No tables, no Redis keys.

## Rules and gotchas
- `common` must never import from a business module (the ArchUnit test checks it).
- No repositories and no entities here.
- The `lesson` contract classes are records. Agent 3 owns their definition, Agent 4 and the Angular models depend on it.

## Status
- [ ] `errors`: ErrorCode, ApiError, handlers, TraceIdFilter
- [ ] `model/enums`: Plan, Subject, Exam, Difficulty
- [ ] `model/lesson`: lesson contract
- [ ] `config`: only if a shared bean is needed
