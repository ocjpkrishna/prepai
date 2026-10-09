# speech

**Purpose:** turns lesson narration into audio the browser can play, and keeps that audio cached so the same sentence is only ever synthesized once.
**Built by:** Agent 4 | **Spec:** 4.5, 4.7, 9.1.1, 9.2 (Agent 4)

## Context
- Audio comes from VoiceStudio, a local TTS server on port 5050. It is synthesized at normal speed; playback speed is applied in the browser.
- Files are named by a SHA-256 of (text, language, voice) and served by nginx from `AUDIO_DIR` at `/audio/{hash}.wav`.
- After a lesson is stored, `lesson` asks this module to warm up all step narrations in the background. A TTS failure must never fail a lesson: the API returns `TTS_UNAVAILABLE` and the player falls back to captions.

## Packages
```
speech/
├── MODULE.md
└── tts/
    ├── config/       AudioProperties (prepai.audio.*), VoiceStudioProperties (prepai.voicestudio.*), TtsConfig
    ├── controller/   TtsController         POST /api/v1/tts/synthesize, GET /api/v1/tts/voices
    ├── service/      TtsService, VoiceStudioClient (interface), HttpVoiceStudioClient, AudioCacheService,
    │                 SingleFlight, AudioKey, WavFormat, TtsWarmupService, AudioCleanupService, AudioCleanupJob
    └── model/        AudioFile, StoredAudio, VoiceStudioRequest, TtsLimits
        └── dto/      SynthesizeRequest, SynthesizeResponse, VoiceDto
```

## Other modules may call
| Class | Used by | Purpose |
|-------|---------|---------|
| `TtsWarmupService.warmUp(narrations, language)` | `lesson` | Prepare audio for a stored lesson in the background (`@Async`, never throws) |

## Data
- Files: audio cache in `AUDIO_DIR` (`prepai.audio.dir`). A file's modification time is its last use.
- Configuration: `prepai.audio.*`, `prepai.voicestudio.*`. Environment: `AUDIO_DIR`, `AUDIO_TTL_DAYS`, `AUDIO_MAX_GB`, `TTS_WARMUP_CONCURRENCY`, `VOICESTUDIO_API_URL`, `VOICESTUDIO_DEFAULT_VOICE`.

## Rules and gotchas
- Single flight is per process: two app instances could each synthesize the same text once. Fine for one VPS.
- Text is limited to 1000 characters per request (`TtsLimits`), enforced by the DTO and again in `TtsService`.
- Writes go to `<hash>.wav.tmp` and are moved into place, so nginx never serves a half file.
- The nightly cleanup (03:00) deletes files unused for `AUDIO_TTL_DAYS`, then the least recently used files above `AUDIO_MAX_GB`. Only `*.wav` files are counted.
- Duration comes from the WAV header, so VoiceStudio does not have to report it. A reply that is not WAV is `TTS_UNAVAILABLE`.
- VoiceStudio sits behind `VoiceStudioClient`; tests use `FakeVoiceStudioClient`, and `HttpVoiceStudioClientTest` uses a local `HttpServer` (never the real server).
- The VoiceStudio HTTP contract is assumed, because its source is not in this repository: `POST /synthesize` with `{text, language, voice}` returns WAV bytes; `GET /voices` returns `[{id, name, language}]`.
- Follow spec 9.1.2. `./gradlew check` must be green.

## Definition of done
Identical text is synthesized once and then served from cache. With VoiceStudio stopped, TTS returns `TTS_UNAVAILABLE` and lessons still generate. The cleanup job respects the age and size limits.

## Status
- [x] synthesize and voices endpoints
- [x] audio cache with single flight
- [x] background warm-up
- [x] nightly cleanup job
- [x] Tests mirrored under `src/test/java/.../speech/`
- [ ] per-user TTS rate limit (spec 4.5 "Limits"): needs the `quota` module, not in row 16
- [ ] `TTS_WARMUP` call from `lesson` (row 13)
- [ ] live VoiceStudio check (no server in the sandbox)
