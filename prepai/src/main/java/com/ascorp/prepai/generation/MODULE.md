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
│   │                 LLMProvider (interface), ClaudeProvider, LocalLLMProvider,
│   │                 GenerationRetryService (2 attempts, repair prompt, backoff), LlmMetrics
│   ├── prompt/       PromptTemplateService     system and user prompts (spec 6.1, 6.2)
│   └── model/        LessonGenerationResult, GenerationMetadata (provider, tokens, cost, retried, attempts)
├── validation/
│   ├── service/      LessonValidator           runs every ValidationRule and collects the errors
│   ├── rule/         ValidationRule (interface), ParseRule, StructureRule, CanvasRule, TextSafetyRule
│   │                                           one class per layer of spec 2.6; a new check is a new bean,
│   │                                           Spring injects them all as a List (open/closed)
│   └── model/        ValidationError, ValidationLayer
├── embedding/
│   └── service/      EmbeddingService          local ONNX model
├── rag/
│   ├── service/      RagService, TextNormalizer, NumericSignatureService
│   ├── repository/   ProblemEmbeddingRepository   pgvector through hibernate-vector
│   └── model/entity/ ProblemEmbedding
├── cache/
│   ├── service/      LessonCacheService
│   └── repository/   LessonCacheRepository     Redis, key lesson:cache:{inputHash}
└── imageextract/
    ├── service/      ImageExtractionService, ImageSanitizer (magic bytes, EXIF strip)
    └── model/        ExtractedProblem, Confidence
```
Not yet owned by any task card: the nightly verifier and corrections job from spec 8.1. A `quality/` feature package in this module is the natural home. Confirm the owner before building it.

## Other modules may call
| Class | Used by | Purpose |
|-------|---------|---------|
| `LessonGenerationService` | `lesson` | `LessonRequest` in, validated `LessonResponse` plus metadata out |
| `ImageExtractionService` | `lesson` | image bytes in, `ExtractedProblem` out |
| `LessonCacheService` | `lesson` | evict a cached lesson (used by the verifier on corrections) |

`LessonRequest`, `LessonResponse` and their nested classes are defined in `common/model/lesson` (spec 3.1, 3.2). This module owns their definition.

## Data
- Tables: `problem_embeddings` (V5), `quality_corrections` (V6). Redis key: `lesson:cache:{inputHash}` (TTL 7 days).
- Configuration: `spring.ai.anthropic.*`, `spring.ai.embedding.transformer.*`, `resilience4j.circuitbreaker.instances.claude.*`, `prepai.llm.*`, `prepai.rag.*`, `prepai.image.*`, `prepai.quality.*`. Environment: `CLAUDE_API_KEY`, `CLAUDE_MODEL`, `LLM_TIMEOUT_SECONDS`, `LLM_MAX_ATTEMPTS`, `LLM_BREAKER_*`, `IMAGE_MAX_BYTES`, `RAG_MIN_SIMILARITY`, `EMBEDDING_*`, `QUALITY_*`, `VERIFIER_MODEL`.

## Rules and gotchas
- Maximum 2 LLM calls per request. Never retry in a loop.
- Student text goes inside `<problem>` tags and is treated as data (spec 6.1, 6.2).
- Images are processed in memory only: never written to disk or the database.
- Use Anthropic prompt caching for the stable system prompt and few-shot examples.
- This module never touches rate limits or stores lessons; `lesson` does.
- Map `problem_embeddings` with `hibernate-vector`, not Spring AI's VectorStore.
- Follow spec 9.1.2. Claude, the embedding model and Redis each sit behind a small class that tests can replace.

## Definition of done
`LessonGenerationService` returns a valid `LessonResponse` for a physics problem. Garbage JSON on the first attempt triggers a repair retry that succeeds; two failures raise `LESSON_GENERATION_FAILED` and nothing is stored. A problem that differs only in its numbers never returns a cached solution. A blurry image raises `IMAGE_UNREADABLE`. A RAG hit returns without any LLM call. Metrics from spec 8.4 are exported.

## Status
- [ ] `common/model/lesson` contract and enums (shared with `common`)
- [ ] `validation`: all layers of 2.6 as rules
- [ ] `llm`: Claude provider, prompts, retry and repair, circuit breaker, metrics
- [ ] `embedding`, `rag`, `cache`
- [ ] `imageextract`
- [ ] `LessonGenerationService` facade
- [ ] Flyway migrations V5, V6
- [ ] Owner confirmed for the nightly verifier (8.1)
- [ ] Tests mirrored under `src/test/java/.../generation/`
