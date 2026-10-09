# lesson

**Purpose:** the lesson lifecycle API that students use to learn: generate a lesson, fetch it, browse history, rate it, and answer its mastery check. It is also the only place where the other modules are tied together into one lesson request.
**Built by:** Agent 4 | **Spec:** 3.1.1, 4.2, 4.7, 5.1 (`lessons`), 9.1.1, 9.2 (Agent 4)

## Context
- `LessonService.generateLesson` is the showcase method of the codebase: check the account, reserve a session, generate, store, commit, warm up audio. It is written as a short list of named steps (spec 9.1.2), with the reservation in a `try` block so any failure releases the session.
- System failures never use up a student's quota. A session is committed only after the lesson is stored.
- Image input goes through `generation.ImageExtractionService` first; the student can confirm or edit the extracted text (`POST /lessons/extract`).

## Packages
```
lesson/
├── MODULE.md
└── lesson/
    ├── controller/   LessonController (generate), LessonExtractController (extract),
    │                 LessonHistoryController (history, get), LessonFeedbackController (feedback),
    │                 MasteryCheckController (mastery-check). Split to stay within the fan-out limit (15).
    ├── service/      LessonService (orchestration), LessonHistoryService, MasteryCheckService
    ├── repository/   LessonRepository
    ├── model/entity/ Lesson
    ├── model/dto/    LessonSummaryDto, FeedbackRequest, MasteryCheckRequest, MasteryCheckResponse,
    │                 ExtractResponse
    └── mapper/       LessonMapper
```
The entity refers to the user by `UUID userId`, never by `account`'s `User`.

## Uses these modules
| Module | Class | Why |
|--------|-------|-----|
| `account` | `AccountGateService`, `UserService` | May this account generate? Which plan? |
| `quota` | `RateLimiterService`, `ExtractLimiter`, `UsageService` | Reserve and commit a session; record usage |
| `generation` | `LessonGenerationService`, `ImageExtractionService` | The lesson itself; text from a photo |
| `speech` | `TtsWarmupService` | Prepare narration audio in the background |
| `common` | `ApiException`, `ErrorCode`, lesson contract | Errors in the 4.7 format; request and response models |

## Data
- Table: `lessons` (V2). It records `source`, `retried`, `retry_reason`, `validation_attempts` and `generation_ms` (spec 5.1).
- Configuration: `prepai.image.max-bytes`.

## Rules and gotchas
- Controllers never touch repositories and never return entities.
- Follow spec 9.1.2: this module is where story-style code matters most.
- `./gradlew check` must be green before a task is marked done.

## Definition of done
Full lifecycle works: generate, store, retrieve, rate, mastery check. The extract endpoint rejects a 6 MB file and a non-image with the right error codes. A failed generation does not change the student's session count.

## Status
- [ ] generate with orchestration (row 13)
- [x] get; history (newest first, subject filter, page size capped at 50); feedback (1 to 5 stars, comment up to 2000 characters); mastery check (row 14)
- [ ] image extract endpoint and the `IMAGE` input type
- [ ] Flyway migration V2
- [ ] Tests mirrored under `src/test/java/.../lesson/`
