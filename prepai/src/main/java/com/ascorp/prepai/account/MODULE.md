# account

**Purpose:** answers "who is this student, and are they allowed in?": sign-up, login, tokens, Google sign-in, the profile, age and consent rules, data export and account deletion.
**Built by:** Agent 2 | **Spec:** 2.7, 4.1, 4.3, 4.7, 5.1 (`users`, `refresh_tokens`, `verification_tokens`), 9.1.1, 9.2 (Agent 2)

## Context
- Auth is stateless JWT through Spring Security's OAuth2 resource server (Nimbus). Access tokens are short-lived (`JWT_EXPIRY_HOURS`, default 1) and refresh tokens rotate. Google sign-in verifies a Google ID token sent by the frontend, then issues PrepAI's own tokens.
- Many students are minors. Under-18 accounts cannot generate lessons until a guardian confirms by email (`CONSENT_REQUIRED`), and every account must verify its email (`EMAIL_NOT_VERIFIED`). See spec 2.7.
- Sign-up throttling lives here (per-IP counter in Redis), so `account` needs nothing from `quota`.

## Packages
```
account/
├── MODULE.md
├── auth/
│   ├── controller/   AuthController           register, login, refresh, google, verify-email
│   ├── service/      AuthService, TokenService, GoogleTokenVerifier,
│   │                 EmailVerificationService, SignupThrottleService
│   ├── repository/   RefreshTokenRepository, VerificationTokenRepository, SignupThrottleRepository (Redis)
│   ├── model/entity/ RefreshToken, VerificationToken
│   ├── model/dto/    RegisterRequest, LoginRequest, RefreshRequest, GoogleLoginRequest, TokenResponse
│   └── config/       SecurityConfig           filter chain, JwtEncoder/JwtDecoder, CORS, public endpoints
├── user/
│   ├── controller/   UserController           GET/PUT /users/me, export, DELETE /users/me
│   ├── service/      UserService, UserDataExportService, AccountDeletionService
│   ├── repository/   UserRepository
│   ├── model/entity/ User
│   ├── model/dto/    UserDto, PreferencesRequest, UserExportDto
│   └── mapper/       UserMapper
└── privacy/
    ├── controller/   GuardianConsentController   confirm link sent to the guardian
    ├── service/      ConsentService (terms, age gate, guardian consent),
    │                 AccountGateService (assertMayGenerate: verified email + consent),
    │                 AccountPurgeJob, RetentionPurgeJob
    └── model/dto/    ConsentRequest
```

## Other modules may call
| Class | Used by | Purpose |
|-------|---------|---------|
| `AccountGateService` | `lesson` | Is this account allowed to generate lessons? |
| `UserService` | `quota`, `lesson`, `billing` | Read a user's plan; `billing` also changes the plan after a payment |

## Data
- Tables: `users` (V1), `refresh_tokens` (V7), `verification_tokens` (V8).
- Redis keys: `signup:{ip}:{hour}`.
- Configuration: `prepai.security.jwt.*`, `prepai.security.google.client-id`, `prepai.privacy.*`, `prepai.app.*`, `spring.mail.*`. Environment: `JWT_SECRET`, `JWT_EXPIRY_HOURS`, `JWT_REFRESH_EXPIRY_DAYS`, `GOOGLE_CLIENT_ID`, `SMTP_*`, `MAIL_FROM`, `GUARDIAN_CONSENT_REQUIRED_UNDER_AGE`, `SIGNUPS_PER_IP_PER_HOUR`, `LESSON_RETENTION_DAYS`.

## Rules and gotchas
- Passwords use BCrypt. Refresh and verification tokens are stored only as SHA-256 hashes. Reuse of a rotated refresh token revokes its whole family.
- Never log emails, tokens or request bodies; log user UUIDs only. Names, emails and user ids are never sent to an LLM provider.
- The `User` entity is private to this module. Other modules get ids and DTOs through `UserService`.
- Follow spec 9.1.2: story-style methods, SOLID, constructor injection, thin controllers. `./gradlew check` must be green.

## Definition of done
Register, verify email, log in, refresh a token, sign in with Google, read and update the profile, export and delete data. A minor cannot generate lessons until the guardian confirms. Errors match spec 4.7. DTOs use MapStruct.

## Status
- [ ] `auth`: register, login, refresh with rotation, Google sign-in, email verification, signup throttle
- [ ] `user`: profile and preferences, export, account deletion
- [ ] `privacy`: terms and age gate, guardian consent, AccountGateService, purge jobs
- [ ] Flyway migrations V1, V7, V8
- [ ] Tests mirrored under `src/test/java/.../account/`
