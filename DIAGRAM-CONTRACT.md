# Diagram contract (physics slice)

Read `DIAGRAM-REDESIGN.md` for the why. This page is the interface the three parallel pieces agree on. Do not change
it without updating all three pieces. Open questions go in the pull request description, never as a silent change.

## 1. Lesson JSON additions (backward compatible)
```json
{
  "scene": { "template": "projectile_horizontal", "params": { "height": 80, "speed": 15, "g": 10 } },
  "steps": [
    {
      "stepNumber": 2,
      "title": "...",
      "narration": "The ball leaves the tower sideways. Gravity pulls it down. The two motions are independent.",
      "reveal":    [ { "element": "tower", "at": 0 }, { "element": "ball", "at": 0 }, { "element": "launch-velocity", "at": 0 } ],
      "emphasise": [ { "element": "height-arrow", "at": 2, "term": "h" } ],
      "equations": [ { "latex": "h = \\tfrac{1}{2} g t^2", "highlight": false, "position": { "x": 520, "y": 120 },
                       "terms": { "h": "height-arrow", "t": "time-label" } } ],
      "canvas": { "actions": [] }
    }
  ]
}
```
- `scene` is optional. A lesson without `scene` keeps using `canvas.actions` exactly as today (fallback).
- With a `scene`, `canvas.actions` may be empty and the validator must not demand 3 to 12 actions per step.
- `reveal`: elements that appear (drawn on) when sentence `at` of the narration starts. Anything not revealed yet stays hidden unless the catalogue says `alwaysVisible` (ground, axes). A step never re-reveals an element; elements persist to the end of the lesson.
- `emphasise`: elements (and the equation symbol `term`) that glow while sentence `at` is spoken.
- `equations[].terms` maps an equation symbol to an element id, so pointing works in both directions.
- Sentences: `narration.split(/(?<=[.!?।])\s+/)`, trimmed, empty ones dropped. `at` is a zero-based index into that list. The same rule is used by the backend validator and the frontend. Keep it in one named helper on each side.
- Every element id must exist in the template's entry in `scene-templates/scene-catalogue.json`; every `param` must respect `min`/`max`/`required`.

## 2. The catalogue
`scene-templates/scene-catalogue.json` is the single source of truth: templates, parameters with limits, the names of
the numbers the template derives, and the element ids. The backend packages it as a classpath resource
(`scene-catalogue.json`); the frontend imports it directly. Neither copies it by hand.

## 3. Frontend drawing contract
Types are in `frontend/src/app/features/lesson/scene/scene.types.ts` (scene space 800 x 600, y down, shapes, style tokens,
`SceneElement`, `BuiltScene`, `TemplateBuilder`, `SceneRef`, `LessonScene`). A builder is a pure function
`(params) => BuiltScene` with no DOM and no randomness. Colours, line weights, fonts and animation timing belong to the
engine's theme, never to a template.

Folders (each piece owns only its own):
- `features/lesson/scene/engine/` the scene engine and the player integration (piece B).
- `features/lesson/scene/templates/` one builder per template plus the `TEMPLATES` registry (piece C).
- `scene.types.ts` and `scene-catalogue.json` are the contract (already committed, change only with care).

## 4. Narration sync
The player asks the speech service for audio **one sentence at a time** (each request is cached by text, so repeats are
free) and knows each sentence's real duration from `durationMs`. When sentence `i` starts, it triggers every `reveal` and
`emphasise` entry with `at == i`. With the browser voice, the sentence start is the utterance start. Reveals bias early,
never late. Pausing pauses the scene clock; changing speed scales it.

## 5. Quality bar
- Hand-drawn look (rough.js), handwriting-style labels, write-on animation for lines, arrows and paths, one palette, three line weights.
- A board that is blank while the narration talks about something drawable is a defect. Piece B ships a Playwright screenshot test that fails if a revealed element leaves the board empty.
- Physically right: templates compute from the parameters (g = 10 by default), never from the model's guesses.
- Everything covered by tests; `./gradlew check` and `ng test` and `ng build` stay green.

## 6. Pieces
| Piece | Owner row | Scope |
|-------|-----------|-------|
| A | 21 | Backend: lesson contract fields, validator, solver, prompt, fake lesson |
| B | 22 | Frontend: scene engine, hand-drawn renderer, sentence-synced playback, term pointing, screenshot test |
| C | 23 | Frontend: the five physics template builders and their tests |
