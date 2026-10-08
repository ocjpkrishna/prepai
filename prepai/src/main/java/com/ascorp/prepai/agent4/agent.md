# Agent 4 - Lesson, Subscription and TTS API

**Model:** Claude Sonnet 5.5 | **Depends on:** Agent 2, Agent 3 | **Spec:** `prepai-spec.md` sections 4.2, 4.4, 4.5, 4.7, 5.1 (`lessons`, `subscriptions`), 9.1.1, 9.2 (Agent 4)

## Purpose
Agent 4 is the part of the API students actually use to learn: it creates and stores lessons, serves history, feedback and mastery checks, takes payments for paid plans, and turns narration text into audio. It is also the only place where the other agents are tied together into one lesson request.

## Context
- `LessonService` orchestrates a request: check the account (Agent 2), reserve a session (Agent 2), generate the lesson (Agent 3), store it, then commit the session on success or release it on any failure. System failures never use up a student's quota.
- Plans and limits come from the shared `Plan` enum in `model/common/enums`. Paid plans are bought through Razorpay (spec 4.4); the webhook updates the user's plan.
- Narration audio comes from VoiceStudio (local TTS on port 5050). Audio is cached by a hash of the text, synthesized at normal speed, and warmed up in the background after a lesson is stored. A TTS failure must never fail a lesson (spec 4.5).

## Package map
```
agent4/
├── agent.md
├── lesson/
│   ├── controller/   LessonController      generate, get, history, feedback, mastery-check, extract
│   ├── service/      LessonService (orchestration), LessonHistoryService, MasteryCheckService
│   ├── repository/   LessonRepository
│   ├── model/entity/ Lesson
│   ├── model/dto/    LessonSummaryDto, FeedbackRequest, MasteryCheckRequest, MasteryCheckResponse,
│   │                 ExtractResponse
│   └── mapper/       LessonMapper
├── subscription/
│   ├── controller/   SubscriptionController (plans, checkout, me), RazorpayWebhookController
│   ├── service/      SubscriptionService, PlanCatalogService, RazorpayGateway (wraps razorpay-java)
│   ├── repository/   SubscriptionRepository
│   ├── model/entity/ Subscription
│   ├── model/dto/    PlanDto, CheckoutRequest, CheckoutResponse, SubscriptionDto
│   └── mapper/       SubscriptionMapper
└── tts/
    ├── controller/   TtsController         POST /tts/synthesize, GET /tts/voices
    ├── service/      TtsService, VoiceStudioClient, AudioCacheService,
    │                 TtsWarmupService, AudioCleanupJob
    └── model/dto/    SynthesizeRequest, SynthesizeResponse, VoiceDto
```
Entities refer to users by `UUID userId`, never by Agent 2's `User` entity.

## Public surface
- REST endpoints from spec 4.2 (lessons), 4.4 (subscriptions) and 4.5 (TTS), all under `/api/v1`.
- No service of this agent is called by another backend agent.

## Calls into other agents
| Agent | Class | Why |
|-------|-------|-----|
| 2 | `AccountGateService` | Is this account allowed to generate? |
| 2 | `RateLimiterService` | reserve, commit, release a session |
| 2 | `UsageService` | write `usage_log` for successful lessons |
| 3 | `LessonGenerationService` | the lesson itself |
| 3 | `ImageExtractionService` | text from a photo |
| 2 | `ApiException`, `ErrorCode` | errors in the 4.7 format |

## Data owned
- Tables: `lessons` (migration V2), `subscriptions` (V4).
- Files: audio cache in `AUDIO_DIR`, named by content hash.

## Configuration
`prepai.audio.*`, `prepai.voicestudio.*`, `prepai.image.max-bytes`, `prepai.razorpay.*`. Environment variables: `AUDIO_DIR`, `AUDIO_TTL_DAYS`, `AUDIO_MAX_GB`, `TTS_WARMUP_CONCURRENCY`, `VOICESTUDIO_API_URL`, `VOICESTUDIO_DEFAULT_VOICE`, `RAZORPAY_*`, `IMAGE_MAX_BYTES`.

## Rules specific to this agent
- Controllers never touch repositories and never return entities.
- A session is committed only after the lesson is stored. Any failure releases the reservation.
- Lessons record `source`, `retried`, `retry_reason`, `validation_attempts` and `generation_ms` (spec 5.1).
- Verify the Razorpay webhook signature and handle webhooks idempotently. This flow is still on the pre-launch TODO list (TODO-3 in spec 15).
- Audio is generated at normal speed only; playback speed is applied in the browser.

## Definition of done
Full lesson lifecycle works: generate, store, retrieve, rate, mastery check. Razorpay checkout creates a subscription and the webhook updates the plan. Identical narration is synthesized once and then served from cache. With VoiceStudio stopped, TTS returns `TTS_UNAVAILABLE` and lessons still generate. The extract endpoint rejects a 6 MB file and a non-image with the right error codes.

## Status
- [ ] `lesson`: generate with orchestration, get, history, feedback, mastery check
- [ ] `lesson`: image extract endpoint and `IMAGE` input type
- [ ] `subscription`: plans, Razorpay checkout, webhook
- [ ] `tts`: synthesize, voices, audio cache, warm-up, nightly cleanup
- [ ] Flyway migrations V2, V4
- [ ] Unit and integration tests mirrored under `src/test/java/.../agent4/`
