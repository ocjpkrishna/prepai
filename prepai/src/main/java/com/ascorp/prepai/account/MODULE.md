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
├── auth/                                  (built)
│   ├── controller/   AuthController           register, login, refresh, google, verify-email
│   ├── service/      AuthService              facade: one method per endpoint
│   │                 PasswordAccountService   sign-up and password check (BCrypt)
│   │                 GoogleSignInService      find or create the account behind a Google ID token
│   │                 TokenService             login tokens, refresh rotation and reuse detection
│   │                 AccessTokenEncoder       signs the JWT access token (subject = user id)
│   │                 EmailVerificationService one-time email links
│   │                 GoogleTokenVerifier      Google JWKS, issuer, audience, email_verified
│   │                 OpaqueTokens             random tokens and their SHA-256 hashes
│   │                 SignupThrottle           signups per IP per hour, counted in Redis (row 5)
│   ├── repository/   UserRepository, RefreshTokenRepository, VerificationTokenRepository
│   ├── model/entity/ User, RefreshToken, VerificationToken
│   ├── model/dto/    RegisterRequest, LoginRequest, RefreshRequest, GoogleLoginRequest, TokenResponse
│   ├── mail/         VerificationMailer (interface), SmtpVerificationMailer (JavaMailSender)
│   └── config/       SecurityConfig (filter chain, CORS, BCrypt), JwtConfig (HS256 encoder and decoder),
│                     AuthProperties (prepai.security.*), SignupProperties (prepai.privacy.signups-per-ip-per-hour),
│                     SignupThrottleInterceptor (counts each POST /api/v1/auth/register), WebConfig (registers it)
├── user/                                  (built, row 4)
│   ├── controller/   UserController         GET/DELETE /users/me, PUT preferences, GET export
│   ├── service/      UserService            profile, preferences, export, plan for other modules
│   ├── mapper/       UserProfileMapper      MapStruct: User entity to UserProfileResponse
│   └── model/dto/    UserProfileResponse, PreferencesRequest
└── privacy/                               (built, row 4)
    ├── controller/   GuardianConsentController   POST /auth/guardian-consent/confirm (public)
    ├── service/      ConsentService         age gate and terms at sign-up
    │                 GuardianConsentService guardian link: send and confirm
    │                 AccountGateService     may this student generate lessons?
    │                 AccountDeletionService soft delete now, purge after 30 days
    │                 PersonalDataService    export and erase across modules (see contributors)
    │                 PurgeJob               nightly purge at 02:15 (PurgeConfig enables scheduling)
    └── config/       PurgeConfig
```

The entity `User` and the repositories stay in `auth` (they are shared by the features of this module); `user` and `privacy` only use them.

## Other modules may call
| Class | Used by | Purpose |
|-------|---------|---------|
| `AccountGateService.requireLessonAccess(userId)` | `lesson` | Throws `EMAIL_NOT_VERIFIED`, `CONSENT_REQUIRED`, `FORBIDDEN` (deletion pending) or `NOT_FOUND`; call it before a session is reserved |
| `UserService.currentPlan(userId)` | `quota`, `lesson`, `billing` | The student's plan |
| `PersonalDataContributor` (in `common/model`) | `quota` (usage), `lesson` (lessons, feedback) | Implement it as a bean: `section()`, `export(userId)`, `erase(userId)`. The export and the purge call every bean |

## Data
- Tables: `users` (V1, plus `voice_speed`, `theme`, `purged_at` in V9), `refresh_tokens` (V7), `verification_tokens` (V8, types `EMAIL` and `GUARDIAN`). Timestamps are `TIMESTAMP WITH TIME ZONE` (see BUILD-DECISIONS.md).
- Redis keys: `signup:{ip}:{hour}` (TTL 1 h), written through `common/redis/RedisCounter`.
- Configuration: `prepai.privacy.signups-per-ip-per-hour` (`SIGNUPS_PER_IP_PER_HOUR`, default 5). The other `prepai.privacy.*` keys belong to the privacy rules.
- Configuration: `prepai.security.*` (`jwt.secret`, `jwt.access-token-hours`, `jwt.refresh-token-days`, `google.client-id`), `prepai.app.*` (`base-url`, `cors-allowed-origins`, `mail-from`), `spring.mail.*`. Environment: `JWT_SECRET`, `JWT_EXPIRY_HOURS`, `JWT_REFRESH_EXPIRY_DAYS`, `GOOGLE_CLIENT_ID`, `SMTP_*`, `MAIL_FROM`, `APP_BASE_URL`, `CORS_ALLOWED_ORIGINS`.

## Rules and gotchas
- Passwords use BCrypt. Refresh and verification tokens are stored only as SHA-256 hashes. Reuse of a rotated refresh token revokes its whole family; `TokenService.rotate` is `noRollbackFor = ApiException` so that revocation is kept when the error goes back to the client.
- Google sign-in joins an existing account only when that account's email is already verified (otherwise `EMAIL_ALREADY_EXISTS`). An empty `GOOGLE_CLIENT_ID` rejects every Google token.
- Login failures use one message for "no such email" and "wrong password" and for Google-only accounts.
- The signup throttle runs in an interceptor, not in the controller, so the controller stays under the fan-out limit (15). It counts the request before its body is validated, and the client address is `getRemoteAddr()`. In production nginx sets `X-Forwarded-For` and `forward-headers-strategy: framework` makes Spring read it; without that every sign-up would count against nginx's address.
- Never log emails, tokens or request bodies; log user UUIDs only. Names, emails and user ids are never sent to an LLM provider.
- The `User` entity is private to this module. Other modules get ids and DTOs through `UserService`.
- Deletion is a soft delete: it sets `deletion_requested_at` and revokes every refresh token at once. A deletion-pending account cannot log in (password or Google), and `AccountGateService` blocks it. The nightly purge (30 days) erases the modules' rows through `PersonalDataContributor`, deletes this module's tokens, and anonymises the `users` row (`purged_at` is set; the row is kept so ids elsewhere still resolve).
- The age gate: `birthDate` only sets `is_minor` and is never stored. Under-18s must give a `guardianEmail`; the guardian gets a 7-day link (`GUARDIAN` token). Until it is confirmed, `AccountGateService` answers `CONSENT_REQUIRED`.
- Google sign-up does not pass the age gate or the terms step yet (no birth date or terms in the Google request). Those accounts are adults with `terms_accepted_at` empty. The frontend and a later change must close this (BUILD-DECISIONS.md, Needs the user).
- The `test` profile has no JPA, so the repositories and the mailer are `@MockitoBean`s in `PrepaiApplicationTests` and `AuthSecurityTest`. The SQL migrations and the entity mapping are not yet checked against PostgreSQL (no `db`-tagged test exists for them).
- Follow spec 9.1.2: story-style methods, SOLID, constructor injection, thin controllers. `./gradlew check` must be green.

## Definition of done
Register, verify email, log in, refresh a token, sign in with Google, read and update the profile, export and delete data. A minor cannot generate lessons until the guardian confirms. Errors match spec 4.7. DTOs use MapStruct.

## Status
- [x] `auth`: register, login, refresh with rotation and reuse detection, Google sign-in, email verification, security config (row 3)
- [x] `auth`: signup throttle in Redis (`SIGNUPS_PER_IP_PER_HOUR`), built in row 5 (`SignupThrottle`, `SignupThrottleInterceptor`). Its Redis behaviour is covered by the `db` test `RedisCounterRedisTest`; the interceptor is tested with a mock throttle.
- [x] `user`: profile, preferences, export, account deletion request (row 4)
- [x] `privacy`: terms and age gate at sign-up, guardian consent link, AccountGateService, deletion and nightly purge (row 4)
- [x] `privacy`: usage in the export and the purge: `UsageDataContributor` in `quota/usage` (row 6)
- [ ] `privacy`: lessons in the export and the purge: the `lesson` `PersonalDataContributor` bean (row 13)
- [ ] `privacy`: the 365-day lesson retention purge (`LESSON_RETENTION_DAYS`), in `lesson` (row 14)
- [x] `user`: `GET /users/me/usage` (row 6, `quota/usage`)
- [x] Flyway migrations V1, V7, V8, V9
- [x] Tests mirrored under `src/test/java/.../account/` (unit tests and the security layer over MockMvc; no database)
- [ ] `db`-tagged integration test for the migrations (V1, V7, V8, V9), `ddl-auto: validate` and the refresh and purge flows (runs on the VPS)
