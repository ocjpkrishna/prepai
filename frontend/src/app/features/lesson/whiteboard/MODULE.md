# features/lesson/whiteboard

**Purpose:** The Konva and KaTeX whiteboard that draws a lesson's canvas actions step by step.
**Built by:** Agent 5 (whiteboard engine) | **Spec:** agent5, 7.1, 3.2

## Context
- Lives under `frontend/src/app/features/` (spec 7.1). Calls the backend only through `core/services`, never with `HttpClient` directly.
- Shared UI (navbar, footer, loading spinner, subject icon) belongs in `shared/`, not here.

## Package map
```
features/lesson/whiteboard/
├── MODULE.md
└── (components, services and routes are added by the owning agent)
```

## Other modules may call
`lesson-player` (inside `features/lesson`) only.

## Data
None. Feature state lives in components and `core/services`.

## Rules and gotchas
- Standalone components only. Angular Material and Konva are used directly, not through wrappers (BUILD-DECISIONS.md, decision 3).
- Follow spec 9.1.2 where it applies to the frontend: short methods, no magic numbers, no `TODO` comments.

## Status
- [ ] components and routes
- [ ] unit tests mirrored under `frontend/src/app/`
