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
├── ratelimit/                             (built, row 5)
│   ├── service/      RateLimiterService     reserveSession(userId) returns a SessionReservation;
│   │                                        commit() keeps it, closing without commit releases it
│   │                 BurstLimiter           at most 5 lesson attempts per minute (RATE_LIMITED)
│   │                 ExtractLimiter         at most 20 photo reads per hour (RATE_LIMITED)
│   ├── repository/   RateLimitRepository    Redis counters, one key per window
│   └── model/        SessionReservation     AutoCloseable
└── usage/                                   (built, row 6)
    ├── controller/   UsageController        GET /users/me/usage
    ├── service/      UsageService           records a delivered session, builds the usage summary
    │                 UsageDataContributor   export and purge of the usage rows (PersonalDataContributor)
    ├── repository/   UsageLogRepository
    ├── model/entity/ UsageLog
    └── model/dto/    UsageResponse, UsageEntryResponse
```

## Other modules may call
| Class | Used by | Purpose |
|-------|---------|---------|
| `RateLimiterService` | `lesson` | Reserve a session; a failed lesson releases it automatically |
| `ExtractLimiter` | `lesson` | `assertWithinExtractLimit(userId)`: image-extract limit (20 per hour per user) |
| `UsageService` | `lesson` | `recordSession(userId, lessonId, durationSeconds)`: record a delivered session, dated in Asia/Kolkata |

## Data
- Table: `usage_log` (V3, row 6). `lesson_id` has no foreign key yet; row 13 adds it with the `lessons` table.
- `usage_log` rows are personal data: `UsageDataContributor` puts them in the export (section `usage`) and erases them on purge.
- Redis keys (spec 5.3), all made by `common/redis/RedisCounter`:
  - `ratelimit:{userId}:{date}`: sessions reserved today, `date` in Asia/Kolkata, expires after 24 h.
  - `ratelimit:burst:{userId}`: attempts in the current minute window, expires after 60 s.
  - `ratelimit:extract:{userId}:{hour}`: photo reads this hour (epoch seconds of the hour start), expires after 1 h.
- Errors it raises: `DAILY_LIMIT_REACHED` (429, `retryAfterSeconds` until the next student midnight), `RATE_LIMITED` (429, burst and extract, with `retryAfterSeconds`) (spec 4.7).

## Rules and gotchas
- `SessionReservation` is `AutoCloseable`, so a caller written as `try (reservation) { ...; reservation.commit(); }` can never leak a session when something fails.
- Every reservation increments the daily counter first. A reservation that is closed without `commit()` decrements it again, and so does a reservation that is refused, so a refused or failed lesson never uses a session. Unlimited plans (`PRO_PLUS`) are never refused, but their sessions are still counted.
- Each counter operation is one Lua script in Redis (`RedisCounter`): the increment and its expiry happen together, so a key never exists without a TTL, even when the app restarts between two commands.
- The burst check runs before the daily count, and every attempt counts toward it, including refused ones, so hammering the endpoint keeps the student blocked until the window ends.
- The burst numbers (5 per minute) and the daily-day boundary (midnight in India) are not in the spec; they are decisions in BUILD-DECISIONS.md.
- Burst limiting returns `retryAfterSeconds` in the error.
- Follow spec 9.1.2. At least 80% line coverage on every service class; `./gradlew check` must be green.

## Definition of done
A free student is stopped on the 4th lesson of the day. A failed generation leaves the count unchanged. `GET /users/me/usage` returns the plan, sessions today, the limit and the per-session minutes.

## Status
- [x] `ratelimit`: reserve, commit, release; burst and extract limits (row 5; the Redis script test `RedisCounterRedisTest` is `db`-tagged and runs on the VPS)
- [x] `usage`: usage log, `GET /users/me/usage`, export and purge (row 6; `sessionsToday` counts `usage_log`, not Redis)
- [x] Flyway migration V3 (row 6; checked against PostgreSQL only by reading, a `db` test is still open)
- [x] Tests mirrored under `src/test/java/.../quota/ratelimit/` and `.../quota/usage/`
