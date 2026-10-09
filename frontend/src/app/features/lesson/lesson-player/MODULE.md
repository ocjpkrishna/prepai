# features/lesson/lesson-player

**Purpose:** Orchestrates the whiteboard, TTS narration and step controls for one lesson.
**Built by:** Agent 6 (TTS and voice sync) | **Spec:** agent6, 7.1, 4.5

## Context
- Lives under `frontend/src/app/features/` (spec 7.1). Calls the backend only through `core/services`, never with `HttpClient` directly.
- Shared UI (navbar, footer, loading spinner, subject icon) belongs in `shared/`, not here.

## Package map
```
features/lesson/lesson-player/
├── MODULE.md
├── lesson-player.component.ts/.html/.scss   the page, route /lessons/:lessonId: loads the lesson, controls, captions
└── lesson-playback.service.ts               plays steps: canvas and narration in parallel, silent mode, speed, pause
core/services/tts.service.ts                 TTSService: synthesize, prefetch, play, pause, available signal
core/mock/mock-audio.ts, mock-lesson.ts      mock backend: silent WAV audio, a 3-step lesson
```

## Other modules may call
Nothing.

## Data
None. Feature state lives in components and `core/services`.

## Rules and gotchas
- Standalone components only. Angular Material and Konva are used directly, not through wrappers (BUILD-DECISIONS.md, decision 3).
- Follow spec 9.1.2 where it applies to the frontend: short methods, no magic numbers, no `TODO` comments.

- A step ends when both the canvas (`stepComplete`) and the narration are done. Pause, speed and the timed caption reveal live in `LessonPlaybackService`; the whiteboard takes `speed` and `paused` inputs.
- Speed is `playbackRate` only; the animation clock is scaled by the same factor. Audio for step N+1 and the summary is requested while step N plays.
- Any TTS failure sets `TtsService.available` to false: captions reveal on a timer, a toast shows, "Retry voice" appears. The Start button is the user gesture for the first audio.
- The synthesize API has no voice parameter, so the voice choice (`pickPreferredVoice`, en-IN first) is only used to read `/tts/voices`; requests use language `en-IN`.
- Not built here: the mastery check (its module is still empty); the summary ends with Replay and a dashboard link.

## Status
- [x] components and routes
- [x] unit tests mirrored under `frontend/src/app/`
- [ ] checked by ear and eye in a browser against the real backend
