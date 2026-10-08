# Agent 2 - Auth, User, Errors, Privacy

**Model:** Claude Sonnet 5.5 | **Depends on:** Agent 1 | **Spec:** `prepai-spec.md` sections 2.7, 4.1, 4.3, 4.7, 5.1 (`users`, `usage_log`, `refresh_tokens`, `verification_tokens`), 5.3, 9.1.1, 9.2 (Agent 2)

## Purpose
Agent 2 answers "who is this student, and what are they allowed to do right now?" It handles sign-up, login, tokens, Google sign-in, the profile, age and consent rules, rate limits, usage counting, and the one error format used by the whole API. It is the foundation the other backend agents build on.

## Context
- Auth is stateless JWT through Spring Security's OAuth2 resource server (Nimbus). Access tokens are short-lived (`JWT_EXPIRY_HOURS`, default 1) and refresh tokens rotate. Google sign-in verifies a Google ID token sent by the frontend and then issues PrepAI's own tokens.
- Many students are minors. Under-18 accounts cannot generate lessons until a guardian confirms by email (`CONSENT_REQUIRED`), and every account must verify its email (`EMAIL_NOT_VERIFIED`). See spec 2.7.
- Every endpoint of every agent returns errors in the shape defined in 4.7. That shape and its handlers live here.
- Agent 4 reserves, commits and releases lesson sessions through this agent's rate limiter. A session counts only when a lesson was really delivered.

## Package map
```
agent2/
├── agent.md
├── auth/
│   ├── controller/   AuthController           register, login, refresh, google, verify-email
│   ├── service/      AuthService, TokenService, GoogleTokenVerifier, EmailVerificationService
│   ├── repository/   RefreshTokenRepository, VerificationTokenRepository
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
├── privacy/
│   ├── controller/   GuardianConsentController   confirm link sent to the guardian
│   ├── service/      ConsentService (terms, age gate, guardian consent),
│   │                 AccountGateService (assertCanGenerate: verified + consent),
│   │                 AccountPurgeJob, RetentionPurgeJob
│   └── model/dto/    ConsentRequest
├── errors/           (no controller, no repository)
│                     ErrorCode, ApiError, ApiException and subclasses, GlobalExceptionHandler,
│                     SecurityErrorHandlers (401/403 in the same shape), TraceIdFilter
├── ratelimit/
│   ├── service/      RateLimiterService (reserve, commit, release), BurstLimiter, SignupThrottleService
│   ├── repository/   RateLimitRepository      Redis
│   └── model/        RateLimitReservation
└── usage/
    ├── controller/   UsageController          GET /users/me/usage
    ├── service/      UsageService
    ├── repository/   UsageLogRepository
    ├── model/entity/ UsageLog
    └── model/dto/    UsageResponse
```
The `Plan` enum (FREE, PRO, PRO_PLUS and their limits) is shared with Agent 4, so it lives in `com.ascorp.prepai.model.common.enums`.

## Public surface (what other agents may call)
| Class | Used by | Purpose |
|-------|---------|---------|
| `ApiException`, `ErrorCode` (`errors`) | every agent | Throw domain errors in the 4.7 format |
| `RateLimiterService` (`ratelimit`) | Agent 4 | reserve / commit / release a daily session |
| `UsageService` (`usage`) | Agent 4 | record a successful session |
| `AccountGateService` (`privacy`) | Agent 4 | verify the account may generate lessons |

## Data owned
- Tables: `users`, `usage_log`, `refresh_tokens`, `verification_tokens` (migrations V1, V3, V7, V8).
- Redis keys: `ratelimit:{userId}:{date}`, `ratelimit:burst:{userId}`, `ratelimit:extract:{userId}:{hour}`, `signup:{ip}:{hour}`.

## Configuration
`prepai.security.jwt.*`, `prepai.security.google.client-id`, `prepai.privacy.*`, `prepai.app.*` and the `spring.mail.*` settings. Environment variables: `JWT_SECRET`, `JWT_EXPIRY_HOURS`, `JWT_REFRESH_EXPIRY_DAYS`, `GOOGLE_CLIENT_ID`, `SMTP_*`, `MAIL_FROM`, `GUARDIAN_CONSENT_REQUIRED_UNDER_AGE`, `SIGNUPS_PER_IP_PER_HOUR`, `LESSON_RETENTION_DAYS`.

## Rules specific to this agent
- Passwords are hashed with BCrypt. Refresh and verification tokens are stored only as SHA-256 hashes. Reuse of a rotated refresh token revokes its whole family.
- Never put emails, tokens or request bodies in logs. Log user UUIDs only.
- Other agents add domain exceptions to `errors`; they never write their own exception handlers.
- Names, emails and user ids are never sent to an LLM provider.

## Definition of done
A student can register, verify their email, log in, refresh a token, sign in with Google, read and update their profile, export and delete their data, and hit the rate limit on the 4th free request. A minor cannot generate lessons until the guardian confirms. All 400/401/403/404/429 responses match spec 4.7. All DTOs use MapStruct.

## Status
- [ ] `errors`: ErrorCode, ApiError, handlers, TraceIdFilter
- [ ] `auth`: register, login, refresh with rotation, Google ID-token sign-in, email verification
- [ ] `user`: profile and preferences, export, account deletion
- [ ] `privacy`: terms and age gate, guardian consent, AccountGateService, purge jobs
- [ ] `ratelimit`: reserve/commit/release, burst limit, signup throttle
- [ ] `usage`: usage log and `GET /users/me/usage`
- [ ] Flyway migrations V1, V3, V7, V8
- [ ] Unit and integration tests mirrored under `src/test/java/.../agent2/`
