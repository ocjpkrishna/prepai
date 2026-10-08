# speech

**Purpose:** turns lesson narration into audio the browser can play, and keeps that audio cached so the same sentence is only ever synthesized once.
**Built by:** Agent 4 | **Spec:** 4.5, 4.7, 9.1.1, 9.2 (Agent 4)

## Context
- Audio comes from VoiceStudio, a local TTS server on port 5050. It is synthesized at normal speed; playback speed is applied in the browser.
- Files are named by a hash of (text, language, voice) and served by nginx from `AUDIO_DIR`.
- After a lesson is stored, `lesson` asks this module to warm up all step narrations in the background. A TTS failure must never fail a lesson: the API returns `TTS_UNAVAILABLE` and the player falls back to captions.

## Packages
```
speech/
├── MODULE.md
└── tts/
    ├── controller/   TtsController         POST /tts/synthesize, GET /tts/voices
    ├── service/      TtsService, VoiceStudioClient, AudioCacheService,
    │                 TtsWarmupService, AudioCleanupJob
    └── model/dto/    SynthesizeRequest, SynthesizeResponse, VoiceDto
```

## Other modules may call
| Class | Used by | Purpose |
|-------|---------|---------|
| `TtsWarmupService` | `lesson` | Prepare audio for a stored lesson without blocking the response |

## Data
- Files: audio cache in `AUDIO_DIR`.
- Configuration: `prepai.audio.*`, `prepai.voicestudio.*`. Environment: `AUDIO_DIR`, `AUDIO_TTL_DAYS`, `AUDIO_MAX_GB`, `TTS_WARMUP_CONCURRENCY`, `VOICESTUDIO_API_URL`, `VOICESTUDIO_DEFAULT_VOICE`.

## Rules and gotchas
- Concurrent requests for the same text are de-duplicated (single flight).
- Text is limited to 1000 characters per request.
- A nightly job removes files unused for `AUDIO_TTL_DAYS` and evicts the least recently used files above `AUDIO_MAX_GB`.
- VoiceStudio sits behind `VoiceStudioClient` so tests never call the real server.
- Follow spec 9.1.2. `./gradlew check` must be green.

## Definition of done
Identical text is synthesized once and then served from cache. With VoiceStudio stopped, TTS returns `TTS_UNAVAILABLE` and lessons still generate. The cleanup job respects the age and size limits.

## Status
- [ ] synthesize and voices endpoints
- [ ] audio cache with single flight
- [ ] background warm-up
- [ ] nightly cleanup job
- [ ] Tests mirrored under `src/test/java/.../speech/`
