# features/legal

**Purpose:** Privacy Policy and Terms pages. Placeholder text until counsel signs off (spec 2.7, TODO-1 in section 15).
**Built by:** Agent 7 (UI/UX) | **Spec:** agent7, 7.1, 2.7

## Context
- Lives under `frontend/src/app/features/` (spec 7.1). Calls the backend only through `core/services`, never with `HttpClient` directly.
- Shared UI (navbar, footer, loading spinner, subject icon) belongs in `shared/`, not here.

## Package map
```
features/legal/
├── MODULE.md
├── legal-page.component.html|scss   shared layout for both pages
├── privacy.component.ts             /privacy
└── terms.component.ts               /terms
```

## Other modules may call
Nothing. The footer and the register page link to these routes.

## Data
None. Text is static.

## Rules and gotchas
- Standalone components only. Angular Material and Konva are used directly, not through wrappers (BUILD-DECISIONS.md, decision 3).
- The text is a draft. Replace it with counsel's text before launch; do not present it as final.

## Status
- [x] components and routes
- [ ] unit tests mirrored under `frontend/src/app/` (smoke test only)
