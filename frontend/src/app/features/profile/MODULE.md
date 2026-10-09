# features/profile

**Purpose:** Profile, settings, data export and account deletion.
**Built by:** Agent 7 (UI/UX) | **Spec:** agent7, 7.1, 2.7

## Context
- Lives under `frontend/src/app/features/` (spec 7.1). Calls the backend only through `core/services`, never with `HttpClient` directly.
- Shared UI (navbar, footer, loading spinner, subject icon) belongs in `shared/`, not here.

## Package map
```
features/profile/
├── MODULE.md
├── profile.component.ts|html|scss   account, language, download my data, delete account (type DELETE)
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
