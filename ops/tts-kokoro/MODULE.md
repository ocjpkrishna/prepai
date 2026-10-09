# TTS service (Kokoro, local)

A small Python sidecar that turns spoken-form text into WAV audio. It sits beside VoiceStudio-style local TTS: Java and the frontend call it over HTTP; it holds no business logic. Python is allowed here because it is a separate process, not part of the Java codebase.

## Run
```
source ~/kokoro/bin/activate
pip install fastapi uvicorn
lsof -i :5051                     # must print nothing
cd <repo>/ops/tts-kokoro
OMP_NUM_THREADS=2 nice -n 15 uvicorn server:app --host 127.0.0.1 --port 5051
```
Models go in `~/kokoro/models` (`kokoro-v1.0.onnx`, `voices-v1.0.bin`). Audio is cached on disk by a hash of voice, speed, language and text.

## Run the UI and the voice together
`serve-ui.py` serves the built UI and proxies `/tts-api/*` to this service, so the browser stays on one origin and the TTS port never leaves localhost:
```
python3 serve-ui.py --dir <repo>/frontend/dist/frontend/browser --port 4300
```
The player asks `/tts-api/voices`; if it answers, it pre-generates one clip per lesson step, uses the clip lengths for the timeline and the audio as the master clock. If not (for example on GitHub Pages), it falls back to the browser voice.

## API
| Call | Result |
|------|--------|
| `GET /health` | status, voice count, cached files |
| `GET /voices` | list of voice names |
| `POST /tts` `{text, voice?, speed?, lang?}` | `audio/wav`, header `X-Cache: hit or miss` |

## Rules
- Text must already be spoken form (see `frontend/src/app/core/services/speech-normalizer.ts`); a Java port belongs in the `speech` module.
- Localhost only. For the browser, add an nginx HTTPS proxy and set `TTS_ALLOWED_ORIGIN`.
- Port 5051 (5050 is VoiceStudio). One synthesis at a time (a lock), about 15 s of audio per 5 s of CPU on this VPS.

## Status
- [x] Kokoro English voices, disk cache, health and voices endpoints
- [x] UI integration: voice list, per-step clips, audio-driven timeline, browser-voice fallback
- [ ] nginx HTTPS proxy, systemd install on the VPS
- [ ] Indic Parler-TTS (Hindi, Indian English) as a second engine
