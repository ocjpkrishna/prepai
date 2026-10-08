# quota

**Purpose:** decides how many lessons a student may start and for how long, per plan, and counts what they actually use.
**Built by:** Agent 2 | **Spec:** 1.4, 4.3, 5.1 (`usage_log`), 5.3, 9.1.1, 9.2 (Agent 2)

## Context
- Plans (Free, Pro, Pro+) set a daily session limit and a per-session time limit. The `Plan` enum with its limits is in `common/model/enums`.
- A session counts only once a lesson was really delivered. Failures caused by the system never use up a student's quota.
- `quota` reads the user's plan through `account.UserService`. `account` does not depend on `quota`.

## Packages
```
quota/
├── MODULE.md
├── ratelimit/
│   ├── service/      RateLimiterService     reserveSession(userId) returns a SessionReservation;
│   │                                        commit() keeps it, closing without commit releases it
│   │                 BurstLimiter, ExtractLimiter
│   ├── repository/   RateLimitRepository    Redis counters
│   └── model/        SessionReservation     AutoCloseable
└── usage/
    ├── controller/   UsageController        GET /users/me/usage
    ├── service/      UsageService           records a successful session, builds the usage summary
    ├── repository/   UsageLogRepository
    ├── model/entity/ UsageLog
    └── model/dto/    UsageResponse
```

## Other modules may call
| Class | Used by | Purpose |
|-------|---------|---------|
| `RateLimiterService` | `lesson` | Reserve a session; a failed lesson releases it automatically |
| `ExtractLimiter` | `lesson` | Image-extract limit (20 per hour per user) |
| `UsageService` | `lesson` | Record a successful session |

## Data
- Table: `usage_log` (V3).
- Redis keys: `ratelimit:{userId}:{date}`, `ratelimit:burst:{userId}`, `ratelimit:extract:{userId}:{hour}`.
- Errors it raises: `DAILY_LIMIT_REACHED`, `RATE_LIMITED` (spec 4.7).

## Rules and gotchas
- `SessionReservation` is `AutoCloseable`, so a caller written as `try (reservation) { ...; reservation.commit(); }` can never leak a session when something fails.
- Burst limiting returns `retryAfterSeconds` in the error.
- Follow spec 9.1.2. At least 80% line coverage on every service class; `./gradlew check` must be green.

## Definition of done
A free student is stopped on the 4th lesson of the day. A failed generation leaves the count unchanged. `GET /users/me/usage` returns the plan, sessions today, the limit and the per-session minutes.

## Status
- [ ] `ratelimit`: reserve, commit, release; burst and extract limits
- [ ] `usage`: usage log and `GET /users/me/usage`
- [ ] Flyway migration V3
- [ ] Tests mirrored under `src/test/java/.../quota/`
