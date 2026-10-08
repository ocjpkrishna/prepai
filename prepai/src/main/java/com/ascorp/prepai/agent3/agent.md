# Agent 3 - LLM Service Layer

**Model:** Claude Sonnet 5.5 | **Depends on:** Agent 1 | **Spec:** `prepai-spec.md` sections 2.4, 2.6, 3.1.1, 3.2, 3.3, 6 (6.1 to 6.3), 8.1, 8.4, 9.1.1, 9.2 (Agent 3)

## Purpose
Agent 3 turns a student's problem into a validated, whiteboard-ready lesson, as cheaply and safely as possible. It decides whether a lesson can come from a cache, calls Claude when it must, checks everything the model returns, and reads text out of photos. It has no REST endpoints: Agent 4 exposes the API and calls this agent through one service.

## Context
- Claude Sonnet 5.5 (`claude-sonnet-5-5`) is the only production LLM. It generates lessons and, with vision, extracts problem text from images. A local Ollama provider exists for development only.
- LLM output is untrusted. Nothing is stored, cached or returned before `LessonValidator` passes it (spec 2.6).
- Order of work for a request: Redis exact-match cache, then the RAG cache, then Claude (at most 2 calls: first attempt, one retry or repair).
- Embeddings are local (`all-MiniLM-L6-v2`, 384 dimensions); Anthropic has no embeddings API.
- The RAG cache serves only verified solutions with the same numbers as the question (spec 6.3). The nightly verifier (8.1) uses Claude Fable 5.1, a different model, to promote rows to verified.

## Package map
```
agent3/
├── agent.md
├── llm/
│   ├── service/      LessonGenerationService   <- the single entry point for Agent 4
│   │                 LLMProvider (interface), ClaudeProvider, LocalLLMProvider,
│   │                 GenerationRetryService (2 attempts, repair prompt, backoff), LlmMetrics
│   ├── prompt/       PromptTemplateService     system and user prompts (spec 6.1, 6.2)
│   └── model/        LessonGenerationResult, GenerationMetadata (provider, tokens, cost, retried, attempts)
├── validation/
│   ├── service/      LessonValidator, HtmlSanitizer (jsoup), LatexDenylist
│   └── model/        ValidationError, ValidationLayer
├── embedding/
│   └── service/      EmbeddingService          local ONNX model
├── rag/
│   ├── service/      RagService, TextNormalizer, NumericSignatureService
│   ├── repository/   ProblemEmbeddingRepository   pgvector, hibernate-vector
│   └── model/entity/ ProblemEmbedding
├── cache/
│   ├── service/      LessonCacheService
│   └── repository/   LessonCacheRepository     Redis, key lesson:cache:{inputHash}
└── imageextract/
    ├── service/      ImageExtractionService, ImageSanitizer (magic bytes, EXIF strip)
    └── model/        ExtractedProblem, Confidence
```
Not yet owned by any task card: the nightly verifier and corrections job from spec 8.1 (a `quality/` package here is the natural home). Confirm the owner before building it.

## Public surface (what other agents may call)
| Class | Used by | Purpose |
|-------|---------|---------|
| `LessonGenerationService` | Agent 4 | `LessonRequest` in, validated `LessonResponse` plus metadata out |
| `ImageExtractionService` | Agent 4 | image bytes in, `ExtractedProblem` out |
| `LessonCacheService` | Agent 4 | evict a cached lesson (used by the verifier on corrections) |

`LessonRequest`, `LessonResponse` and their nested classes are shared with Agent 4 and live in `com.ascorp.prepai.model.common.lesson`. Agent 3 owns their definition (spec 3.1, 3.2).

## Data owned
- Tables: `problem_embeddings` (migration V5), `quality_corrections` (V6).
- Redis keys: `lesson:cache:{inputHash}` (TTL 7 days).

## Configuration
`spring.ai.anthropic.*`, `spring.ai.embedding.transformer.*`, `resilience4j.circuitbreaker.instances.claude.*`, `prepai.llm.*`, `prepai.rag.*`, `prepai.image.*`, `prepai.quality.*`. Environment variables: `CLAUDE_API_KEY`, `CLAUDE_MODEL`, `LLM_TIMEOUT_SECONDS`, `LLM_MAX_ATTEMPTS`, `LLM_BREAKER_*`, `IMAGE_MAX_BYTES`, `RAG_MIN_SIMILARITY`, `EMBEDDING_*`, `QUALITY_*`, `VERIFIER_MODEL`.

## Rules specific to this agent
- Maximum 2 LLM calls per request. Never retry in a loop.
- Student text goes inside `<problem>` tags and is treated as data (spec 6.1, 6.2).
- Images are processed in memory only: never written to disk or the database.
- Use Anthropic prompt caching for the stable system prompt and few-shot examples.
- Never read or write another agent's tables. Rate limiting and storage of lessons belong to Agent 4 and Agent 2.
- Map `problem_embeddings` with `hibernate-vector`, not Spring AI's VectorStore.

## Definition of done
`LessonGenerationService` returns a valid `LessonResponse` for a physics problem. Garbage JSON on the first attempt triggers a repair retry that succeeds; two failures raise `LESSON_GENERATION_FAILED` and nothing is stored. A problem that differs only in its numbers never returns a cached solution. A blurry image raises `IMAGE_UNREADABLE`. A RAG hit returns without any LLM call. Metrics from spec 8.4 are exported.

## Status
- [ ] `model/common` lesson contract (`LessonRequest`, `LessonResponse` and nested classes) and enums
- [ ] `validation`: all layers of 2.6
- [ ] `llm`: Claude provider, prompts, retry and repair, circuit breaker, metrics
- [ ] `embedding`: local ONNX embeddings
- [ ] `rag`: normalisation, numeric signature, verified-only lookup, unverified storage
- [ ] `cache`: Redis exact-match cache
- [ ] `imageextract`: sanitise and extract with Claude vision
- [ ] `LessonGenerationService` facade
- [ ] Flyway migrations V5, V6
- [ ] Owner confirmed for the nightly verifier (8.1)
- [ ] Unit and integration tests mirrored under `src/test/java/.../agent3/`
