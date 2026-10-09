# generation

**Purpose:** turns a student's problem into a validated, whiteboard-ready lesson as cheaply and safely as possible. It decides whether a lesson can come from a cache, calls Claude only when it must, checks everything the model returns, and reads problem text out of photos. It has no REST endpoints: `lesson` exposes the API and calls this module through one service.
**Built by:** Agent 3 | **Spec:** 2.4, 2.6, 3.1.1, 3.2, 3.3, 6 (6.1 to 6.3), 8.1, 8.4, 9.1.1, 9.2 (Agent 3)

## Context
- Claude Sonnet 5.5 (`claude-sonnet-5-5`) is the only production LLM: it generates lessons and, with vision, reads images. A local Ollama provider exists for development only.
- LLM output is untrusted. Nothing is stored, cached or returned before the validator passes it (spec 2.6).
- Order of work for a request: Redis exact-match cache, then the RAG cache, then Claude (at most 2 calls: first attempt, then one retry or repair).
- Embeddings are local (`all-MiniLM-L6-v2`, 384 dimensions); Anthropic has no embeddings API.
- The RAG cache serves only verified solutions with the same numbers as the question (spec 6.3). The nightly verifier (8.1) uses Claude Fable 5.1, a different model, to promote rows to verified.

## Packages
```
generation/
├── MODULE.md
├── llm/
│   ├── service/      LessonGenerationService   <- the single entry point for `lesson`
│   │                 LlmProvider (interface), ClaudeProvider (Spring AI), FakeLessonProvider (dev and test),
│   │                 ClaudeTranslator (prompt and reply mapping, cost), ClaudeFailures (HTTP and timeout
│   │                 mapping, shared), ClaudeVerifierClient + VerifierExchange + VerifierSystemPrompt (Fable,
│   │                 production only), LlmCaller (circuit breaker), GenerationRetryService (2 attempts, repair
│   │                 prompt, pause), LlmMetrics
│   ├── prompt/       PromptTemplateService     system and user prompts (spec 6.1, 6.2), from resources/prompts
│   │                 ProblemTags               strips every form of the <problem> tag from untrusted text
│   └── model/        LessonGenerationResult, GenerationMetadata, GenerationRun, LlmPrompt (optional photo),
│                     LlmImage, LlmPurpose, LlmCompletion, LlmProviderException, RetryReason, LlmProperties (`prepai.llm.*`)
├── validation/
│   ├── service/      LessonValidator           parse, sanitise, then run every ValidationRule
│   │                 LessonSanitizer           strips HTML from every string (jsoup) before the rules
│   ├── rule/         ParseRule                 JSON or out-of-scope answer (not a ValidationRule)
│   │                 ValidationRule (interface), StructureRule, CanvasRule, CanvasBoundsRule, TextSafetyRule
│   │                                           one class per check of spec 2.6; a new check is a new bean,
│   │                                           Spring injects them all as a List (open/closed)
│   └── model/        ValidationResult, ValidationError, ValidationLayer, ValidationCode
├── embedding/
│   └── service/      EmbeddingService          local all-MiniLM-L6-v2 (Spring AI EmbeddingModel)
├── rag/
│   ├── service/      RagService               lookup (verified only) and store (unverified or ask_count)
│   │                 ProblemProbeService      normalise, numeric signature, embed (spec 6.3 steps 1 to 3)
│   │                 ProblemReviewService     verifier's batch, promotion to verified, backlog
│   │                 TextNormalizer, NumericSignatureService, RagMetrics
│   ├── repository/   ProblemEmbeddingRepository   pgvector `<=>` query through hibernate-vector
│   └── model/        ProblemEmbedding (entity, model/entity), ProblemProbe, RagLookupResult, RagProperties,
│                     StoredProblem (what the verifier reads)
├── cache/
│   ├── service/      LessonCacheService       exact match, key lesson:cache:{SHA-256 of the request JSON}
│   └── repository/   LessonCacheRepository     Redis strings, 7-day TTL
├── quality/
│   ├── service/      VerificationService      one nightly batch: grade, then verify or correct (spec 8.1)
│   │                 CorrectionService        logs a wrong answer and its correction, stores the correction
│   │                 NightlyVerifierJob       02:30 cron; VerifierClient (interface); QualityMetrics (gauge)
│   ├── repository/   QualityCorrectionRepository
│   └── model/        VerifierGrade, VerificationReport, QualityProperties (`prepai.quality.*`),
│                     entity/QualityCorrection
└── imageextract/
    ├── service/      ImageExtractionService   sanitise, read once through LlmCaller, parse, refuse LOW
    │                 ImageSanitizer           size, magic bytes, then one MetadataStripper per format
    │                 JpegMetadataStripper, PngMetadataStripper, WebpMetadataStripper (MetadataStripper)
    │                 ExtractionPrompt         vision prompt (resource) with the photo as media
    │                 ExtractionReplyParser    the reader's JSON; anything else is unreadable
    └── model/        ExtractedProblem, Confidence, ImageFormat, SanitizedImage, ImageProperties (`prepai.image.*`)
```

## Other modules may call
| Class | Used by | Purpose |
|-------|---------|---------|
| `LessonGenerationService` | `lesson` | `LessonRequest` in, validated `LessonResponse` plus metadata out |
| `ImageExtractionService` | `lesson` | image bytes in, `ExtractedProblem` out |
| `LessonCacheService` | `lesson` | find and put a cached lesson by request (the verifier does not evict; see the gotchas) |
| `RagService` | `lesson` | `lookup(subject, problemText)` before generating; `store(...)` after a validated lesson |
| `ImageExtractionService` | `lesson` | `extract(bytes)` for an uploaded photo; `ExtractedProblem` out, `IMAGE_UNREADABLE` for a LOW reading |

`LessonRequest`, `LessonResponse` and their nested classes are defined in `common/model/lesson` (spec 3.1, 3.2). This module owns their definition.

## Data
- Tables: `problem_embeddings` (V5), `quality_corrections` (V6, no foreign key on `lesson_id` until row 13). Redis key: `lesson:cache:{inputHash}` (TTL 7 days).
- Configuration: `spring.ai.anthropic.*`, `spring.ai.embedding.transformer.*`, `resilience4j.circuitbreaker.instances.claude.*`, `prepai.llm.*`, `prepai.rag.*`, `prepai.image.*`, `prepai.quality.*`. Environment: `CLAUDE_API_KEY`, `CLAUDE_MODEL`, `LLM_TIMEOUT_SECONDS`, `LLM_MAX_ATTEMPTS`, `LLM_BREAKER_*`, `IMAGE_MAX_BYTES`, `RAG_MIN_SIMILARITY`, `EMBEDDING_*`, `QUALITY_*`, `VERIFIER_MODEL`.

## Rules and gotchas
- Maximum 2 LLM calls per request (`prepai.llm.max-attempts`). Never retry in a loop.
- RAG serves only `verified = true` rows with an identical numeric signature (`5 m/s` and `25 m/s` never match). The lookup reads the top 3 candidates of the same subject with cosine similarity ≥ `prepai.rag.min-similarity`.
- Served RAG and cached lessons keep the `lessonId` they were first given. The caller (`lesson`) must assign a new id before saving the student's lesson.
- Only validated lessons may reach `RagService.store` or `LessonCacheService.put`; nothing is stored before `LessonValidator` passes it.
- The numeric signature is built from NFKC text with the case kept (a real minus sign, superscript and full-width digits are read; `5 mW` and `5 MW` differ). Only the stored text and the embedding are lowercased.
- An unreadable `lesson:cache` entry is a miss, never a failed request. A provider reply with no answer is a `PROVIDER_ERROR`.
- Problem text is normalised before storage and lookup: emails and Indian mobile numbers become `[email]` and `[phone]`, and units after a number are canonicalised. The stored `problem_text` is that normalised text, not the student's original wording.
- A signature longer than 500 characters (the column size) is not cached at all: lookup misses and store does nothing.
- The exact-match key hashes the whole request as JSON (type, subject, exam, difficulty, language, text, image), so two requests share a key only when they are identical.
- Each RAG lookup increments `prepai_rag_lookup_total{result=hit|miss|rejected_numeric}` (spec 8.4). The `prepai_rag_unverified_backlog` gauge is exported by `QualityMetrics` (one count query per scrape).
- Unverified rows are never served; the nightly verifier (row 9b) is the only thing that sets `verified = true`.
- The provider is chosen by `prepai.llm.provider`: `claude` in production (the default), `fake` in `local` and `test` (decision 7). `LocalLLMProvider` (Ollama) is not built: its starter is not in `build.gradle`.
- Only `ClaudeFailures` translates Spring AI errors, for `ClaudeProvider` and `ClaudeVerifierClient`. It maps HTTP 429 to `RATE_LIMITED`, other HTTP errors to `PROVIDER_ERROR`, and read timeouts to `TIMEOUT`. This assumes Spring AI raises Spring's `RestClientResponseException` and `ResourceAccessException`; unverified against the live API (no key, decision 6).
- The nightly verifier is production only (`@Profile("prod")` on `NightlyVerifierJob`, `VerificationService`, `CorrectionService` and `ClaudeVerifierClient`). No test or build step calls Fable; tests use a mocked `VerifierClient`.
- The verifier batch takes unverified rows only, most asked first. The question-bank fill of spec 8.1 step 1 is not built (no question bank in the repo).
- A verifier reply that is not grade JSON counts as a failed call: the row stays unverified and the next night tries it again. One failed row never stops the batch.
- A lesson passes when the answer is correct and step quality is at least 3 (`VerifierGrade.passes`). Anything else is corrected with `LessonGenerationService`, logged to `quality_corrections`, and stored as verified (spec 8.1 step 5).
- The verifier does not evict `lesson:cache` keys (spec 8.1 step 5d). The key hashes the student's original text, which the row does not keep, so an identical exact-match request can get the old lesson until its 7-day TTL ends. The RAG row itself is corrected at once. Open for the user, see BUILD-DECISIONS.md.
- The circuit breaker (`resilience4j` instance `claude`) wraps provider calls only. Validation failures never open it. While it is open the student gets `LLM_UNAVAILABLE` and no call is made.
- A transient failure is retried with the plain prompt after `prepai.llm.retry-backoff`. A validation failure is retried at once with a repair prompt that lists the validator's messages.
- Cost is computed at Sonnet 5.5 list prices from input and output tokens; cache read and write tokens are not priced separately (an approximation).
- `LessonGenerationService` covers the LLM path only; the Redis and RAG lookups come in rows 9 and 13.
- Student text goes inside `<problem>` tags and is treated as data (spec 6.1, 6.2).
- Images are processed in memory only: never written to disk or the database.
- Photos: size first (`IMAGE_TOO_LARGE`, 413), then magic bytes (`IMAGE_UNSUPPORTED`, 415; a Content-Type is never trusted). A broken file with the right magic bytes is also `IMAGE_UNSUPPORTED`. Metadata is stripped before the model sees the photo: JPEG keeps APP0, APP2 (colour profile) and APP14 and drops every other APPn and comments; PNG drops eXIf and text chunks and stops at IEND; WebP drops EXIF and XMP chunks and rewrites the RIFF size.
- Extraction is one model call, with no retry (spec 3.1.1 names none). Breaker open or a provider failure gives `LLM_UNAVAILABLE`. A LOW confidence, an empty text or a reply that is not the JSON object gives `IMAGE_UNREADABLE`, and that reply never reaches the student.
- `LlmPrompt.purpose()` is `IMAGE_EXTRACT` when the prompt carries a photo, so the spec 8.4 metrics label extraction calls without a separate path.
- Use Anthropic prompt caching for the stable system prompt and few-shot examples.
- This module never touches rate limits or stores lessons; `lesson` does.
- Map `problem_embeddings` with `hibernate-vector`, not Spring AI's VectorStore.
- Follow spec 9.1.2. Claude, the embedding model and Redis each sit behind a small class that tests can replace.
- Every model response goes through `LessonValidator.validate`; a rule returns errors and never throws. The sanitiser runs before the rules, so they check the text the student sees.

## Definition of done
`LessonGenerationService` returns a valid `LessonResponse` for a physics problem. Garbage JSON on the first attempt triggers a repair retry that succeeds; two failures raise `LESSON_GENERATION_FAILED` and nothing is stored. A problem that differs only in its numbers never returns a cached solution. A blurry image raises `IMAGE_UNREADABLE`. A RAG hit returns without any LLM call. Metrics from spec 8.4 are exported.

## Status
- [ ] `common/model/lesson` contract and enums (shared with `common`)
- [x] `validation`: all layers of 2.6 as rules
- [x] `llm`: Claude provider, prompts, retry and repair, circuit breaker, metrics (fake provider for dev; `LocalLLMProvider` not built)
- [x] `embedding`, `rag`, `cache` (services and repositories; `lesson` wires the lookups in row 13)
- [x] `imageextract`: sanitising (size, magic bytes, EXIF and text stripping per format), vision read through `LlmCaller`, `IMAGE_UNREADABLE` rules; the `lesson` endpoint and the `IMAGE` input path come in rows 13 and 17
- [x] `LessonGenerationService` facade (LLM path; the cache and RAG calls are made by `lesson`, row 13)
- [x] Flyway migration V5 (`problem_embeddings`, pgvector)
- [x] Flyway migration V6 (`quality_corrections`, row 9b; the `db` test runs with out-of-order enabled, see decisions)
- [x] Nightly verifier and corrections (`quality`, row 9b); owner is this module (BUILD-DECISIONS.md, decision 2)
- [x] Tests mirrored under `src/test/java/.../generation/`
