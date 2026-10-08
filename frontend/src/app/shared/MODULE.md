# shared

**Purpose:** Components, pipes and directives used by more than one feature (navbar, footer, loading spinner, subject icon, duration pipe).
**Built by:** Agent 7 (UI/UX) | **Spec:** agent7, 7.1

## Context
- Lives under `frontend/src/app/features/` (spec 7.1). Calls the backend only through `core/services`, never with `HttpClient` directly.
- Shared UI (navbar, footer, loading spinner, subject icon) belongs in `shared/`, not here.

## Package map
```
shared/
├── MODULE.md
└── (components, services and routes are added by the owning agent)
```

## Other modules may call
Every feature may use it. It may not import from a feature.

## Data
None. Feature state lives in components and `core/services`.

## Rules and gotchas
- Standalone components only. Angular Material and Konva are used directly, not through wrappers (BUILD-DECISIONS.md, decision 3).
- Follow spec 9.1.2 where it applies to the frontend: short methods, no magic numbers, no `TODO` comments.

## Status
- [ ] components and routes
- [ ] unit tests mirrored under `frontend/src/app/`
