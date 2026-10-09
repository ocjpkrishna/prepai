# Dudely reference notes

**Purpose.** Dudely (dudely.co) is a live AI tutor with an animated whiteboard. We use it as a **reference for user experience and architecture patterns only**. We do not copy its code, prompts, fonts, assets or branding; PrepAI's design, vocabulary and code are our own.

**Source and limits.** Everything here comes from (1) a 20-second screen recording of the home and lesson screens and (2) a browser HAR of the landing-page load, which contains Dudely's public client JavaScript bundle (about 1.5 MB). The HAR holds no lesson traffic, so the server side (prompts, model, message contents) is unknown. Items marked *inferred* are conclusions from the client code, not observed behaviour.

## 1. What the user sees

- **Home:** serif heading "What are we learning today?", one prompt box (attach button, Start), "Your sessions" with Continue, and four mode cards: Cram for an exam, Homework help, Explain it simply, Practice problems.
- **Lesson screen:** an infinite, dotted-grid board. The tutor writes in a green handwriting font, drawn by a blue marker cursor with a "Dudely" name tag. The student's cursor is a marker tagged "You".
- **Controls (top right):** Whiteboard / Chalkboard theme toggle, Sound, Tap to talk (mic).
- **Bottom bar:** text chat bar with attach and Send; a banner for the free-minutes limit with Upgrade to Pro.
- **Free tier:** a daily minutes allowance (3 minutes seen), tracked by `/api/usage`.

## 2. Technology (from the bundle)

| Area | Finding |
|------|---------|
| App | Vite single-page app, React, about 1.5 MB JavaScript |
| Board | Plain **SVG**, no canvas library (no Excalidraw, Konva or tldraw) |
| Hand-drawn look | **rough.js** is bundled (`roughness`, `fillSketch`, `opsToPath`) |
| Drawing-in animation | `getTotalLength()` plus `stroke-dasharray` / `stroke-dashoffset` on each SVG path |
| Text | Caveat handwriting font (variable), Instrument Serif and SF Pro for the UI |
| Math | KaTeX |
| Voice (live) | WebSocket `/api/live`, audio through an AudioWorklet (two-way realtime) |
| Voice (fallback) | `POST /api/tts` with `{text, voice}`, plus browser `speechSynthesis` |
| Vision | `POST /api/read` (reads an uploaded image or a region of the student's ink) |
| Images | `POST /api/image` (AI illustration from a prompt) |
| Other endpoints | `/api/usage`, `/api/geo`, `/api/health`, `/api/checkout-token`, `/api/portal`, `/api/switch-plan`, `/api/cancel`, `/api/uncancel` |
| Analytics | PostHog (events, session recording, surveys) |
| Sign-in | Google Identity Services plus email and password |

## 3. How the tutor draws: tool calls, not coordinates

The tutor model draws by calling named tools (*inferred* from the client's tool dispatcher). It never sends pixel positions.

**Tool names seen:** `write`, `draw_code`, `sketch`, `diagram`, `generate_image`, `fill`, `connect`, `emphasize`, `highlight`, `erase`, `move`, `new_page`, `clear_board`, `plan_lesson`, `begin_concept`.

**Key properties**
1. **Every object has a `ref`.** `write`, `sketch`, `diagram` and `image` take a `ref`. Later calls point at it (`connect(from, to, label)`, `emphasize(target, part, color)`, `erase(target)`, `move(target)`). Reusing a `ref` replaces the object.
2. **The client lays everything out.** The model says what and relative to what; the client decides where. It also auto-scrolls to follow the pen.
3. **One continuous notebook.** `new_page` is ignored ("the notebook never wipes"); the board only grows.
4. **Lesson plan first.** `plan_lesson` gives concepts in teaching order; `begin_concept(id)` moves to the next. Unknown ids return an error naming the valid ids.
5. **Paused lessons.** Drawing calls made while paused are replayed afterwards; other calls are refused with a message.
6. **Text versus math.** `write` has `kind: text` (prose, handwriting font) or `kind: math` (KaTeX). A sentence sent as math is rejected with a correction.
7. **Spoken references.** Phrases like "the second equation" or "step three" are resolved to board items.

### Named `sketch` shapes
axes, curve, line, arrow, box, circle, bracket, table, number_line, triangle (scalene, isosceles, equilateral, right), right_triangle, square, pentagon, hexagon, sine_wave, square_wave, sawtooth, parabola, freeform.

### `diagram` kinds (auto-laid-out)
flow, tree, network, columns, timeline.

### Other drawing sources
`generate_image` for real physical things; `draw_code` as a code-drawn escape hatch for anything else.

## 4. Self-correcting tool replies

Each tool call returns a short text result to the model, either `ok: ...` or `error: ...`. Examples of the pattern:
- `error: <reason>, so NOTHING was drawn, do not refer to it. Draw it now with <fix>. Never fall back to a bare line.` This stops narration describing a figure that is not on the board.
- Layout checks: a connector whose endpoints are not laid out yet, or are too far apart ("an arrow would cross the board"), is refused with the distance.
- Plan checks: `begin_concept` before `plan_lesson` returns `error: call plan_lesson before begin_concept`.
- Narration-versus-board checks: if the narration talks about a trend (grow, fall, exponential) or a wave (sine, frequency, amplitude), the client expects a matching graph or wave shape on the board.

## 5. Quality counters (client telemetry)

The client counts tutor mistakes per session: role leaks, stage directions, tool-call leaks, apologies, repeats, restarts, recovery loops, grounding refusals, monologues over budget, "cannot see" claims, aliases, board clears and confusion signals. Useful as a model for our own lesson-quality metrics.

## 6. Student ink

The student can write on the board with a pen. The strokes are captured, cached by signature, and read by the vision endpoint to check the student's steps. Circling gestures are detected and reported as context ("react to that thing").

## 7. What we take for PrepAI (decided 2026-10-09)

| Topic | Decision |
|-------|----------|
| Renderer | SVG with rough.js and stroke draw-in, replacing Konva. Marker cursor follows the active stroke. |
| Handwriting | Handwriting font with a left-to-right reveal, not true stroke paths. Caveat is Latin only, so PrepAI also needs a font with Devanagari (candidate: Kalam). The UI font must not be SF Pro (proprietary). |
| Board | One continuous notebook; `new_page` ignored; `clear_board` is an explicit reset only. |
| Vocabulary | Dudely's `write`, `sketch`, `diagram`, `image`, `connect`, `emphasize`, `highlight`, `fill`, `erase`, `move`, `plan_lesson`, `begin_concept` set as the base. JEE and NEET kinds (free-body, circuits, ray optics, chemistry structures, building and ground scenes) are added after launch, one at a time. |
| Validator | The `ok:` / `error:` self-correcting reply pattern is the design of our lesson validator, with one repair round. |
| Delivery | Pre-generated, cached lessons (not realtime two-way voice). Narration comes from VoiceStudio TTS with caption fallback. Realtime voice is out of scope for now. |
| Cache | First answers, scene descriptions, TTS audio and images are cached. Follow-ups are not. |
| Provider | LLM provider is still TODO-9. Until a key exists, the build uses the fake lesson provider. |

## 8. Not copied

Dudely's code, prompts, fonts (SF Pro, Instrument Serif), icons, copy, colours and branding. Their tool names are used as a vocabulary reference; ours are defined in spec 3.3.
