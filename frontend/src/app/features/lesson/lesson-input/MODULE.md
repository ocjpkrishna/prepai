# features/lesson/lesson-input

**Purpose:** The form where a student types or photographs a problem and asks for a lesson.
**Built by:** Agent 7 (UI/UX) | **Spec:** agent7, 7.1, 3.1

## Context
- Lives under `frontend/src/app/features/` (spec 7.1). Calls the backend only through `core/services`, never with `HttpClient` directly.
- Shared UI (navbar, footer, loading spinner, subject icon) belongs in `shared/`, not here.

## Package map
```
features/lesson/lesson-input/
├── MODULE.md
├── lesson-input.component.ts|html|scss  text question, photo flow, "We read this as…" review, generate
├── lesson-input.options.ts          exam and difficulty options, MAX_TEXT_LENGTH
└── image-prep.ts                    type check, downscale to 1600 px as JPEG, 5 MB limit
```

## Other modules may call
Nothing.

## Data
None. Feature state lives in components and `core/services`.

## Rules and gotchas
- Standalone components only. Angular Material and Konva are used directly, not through wrappers (BUILD-DECISIONS.md, decision 3).
- Follow spec 9.1.2 where it applies to the frontend: short methods, no magic numbers, no `TODO` comments.

## Status
- [x] components and routes
- [x] unit tests mirrored under `frontend/src/app/`
