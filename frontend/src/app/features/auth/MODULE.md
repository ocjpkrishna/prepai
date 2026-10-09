# features/auth

**Purpose:** Login and register screens, with the auth routes.
**Built by:** Agent 7 (UI/UX) | **Spec:** agent7, 7.1, 4.1

## Context
- Lives under `frontend/src/app/features/` (spec 7.1). Calls the backend only through `core/services`, never with `HttpClient` directly.
- Shared UI (navbar, footer, loading spinner, subject icon) belongs in `shared/`, not here.

## Package map
```
features/auth/
├── MODULE.md
├── auth.routes.ts                   /login, /register, /verify-email, /guardian-pending, /guardian-consent
├── age-gate.ts                      isMinor(dateOfBirth), the under-18 check (spec 2.7)
├── return-url.ts                    safeReturnUrl(): only same-site paths after login
├── auth-form.scss                   form layout shared by the auth pages
├── login/                           email and password; Google button is a disabled placeholder
├── register/                        name, email, password, date of birth, guardian email for minors, terms
├── verify-email/                    "check your inbox", or verifies the emailed token
├── guardian-pending/                "waiting for guardian consent" screen (CONSENT_REQUIRED)
└── guardian-consent/                guardian's public consent page (token from the email)
```

## Other modules may call
Nothing. Auth state lives in `core/auth`.

## Data
None. Feature state lives in components and `core/services`.

## Rules and gotchas
- Standalone components only. Angular Material and Konva are used directly, not through wrappers (BUILD-DECISIONS.md, decision 3).
- Follow spec 9.1.2 where it applies to the frontend: short methods, no magic numbers, no `TODO` comments.

## Status
- [x] components and routes
- [x] unit tests mirrored under `frontend/src/app/`
