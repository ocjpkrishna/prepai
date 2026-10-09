# features/lesson/whiteboard

**Purpose:** The Konva and KaTeX whiteboard that draws a lesson's canvas actions step by step.
**Built by:** Agent 5 (whiteboard engine) | **Spec:** agent5, 7.1, 3.2, 3.3

## Context
- Lives under `frontend/src/app/features/` (spec 7.1). Calls the backend only through `core/services`, never with `HttpClient` directly. It needs no backend call: it draws the `LessonStep` it is given.
- Shared UI (navbar, footer, loading spinner, subject icon) belongs in `shared/`, not here.
- The lesson and action models are in `core/models/` (`lesson.model.ts`, `canvas-action.model.ts`), as spec 7.1 places them.

## Package map
```
features/lesson/whiteboard/
├── whiteboard.component.ts/.html/.scss   the public component: step, theme in; stepComplete, renderWarning out
├── whiteboard-equation.component.ts      one KaTeX equation in the panel
├── canvas-renderer.service.ts            plays a step: fade, then each action in turn; warnings
├── animation-engine.service.ts           one frame loop at a time, easing, cancel
├── equation-renderer.service.ts          KaTeX with trust off; raw source on parse error; fit to width
├── whiteboard-scene.ts                   the Konva layer, one group per action id, the LaTeX overlay, the pen
├── draw-context.ts                       the Drawer type
├── action-config.ts                      safe reader for an action's config (fallbacks, clamps, notes)
├── expression.ts                         parser for graph functions; no eval
├── geometry.ts, easing.ts                points, arcs, curves, staggering; easing functions
├── konva-shapes.ts                       re-exports Konva 10's classes by name
├── whiteboard.constants.ts, whiteboard.types.ts
├── drawers/                              one drawer per action type (shape, text, scene drawers) and the registry
├── testing/                              Konva stand-in, spec 3.2 sample, every-type step, malformed step
└── *.spec.ts                             unit tests, next to the code they test
```

## Other modules may call
`lesson-player` (inside `features/lesson`) only. It passes the current `LessonStep` and the theme, and listens to `stepComplete`.

## Data
None. Feature state lives in the component and its services.

## Rules and gotchas
- Standalone components only. Angular Material and Konva are used directly, not through wrappers (BUILD-DECISIONS.md, decision 3). Import Konva classes from `konva-shapes.ts`, not from `konva`.
- Never throw on bad data. A bad action is skipped (or drawn with a fallback), a `RenderWarning` is emitted and `console.warn` is logged, and the step plays on. Only the renderer's own loop can stop a step, and only a newer step, `destroy()` or a frame error does that.
- Coordinates are canvas units (800 by 500). Points are clamped to the canvas; the canvas scales to its width.
- KaTeX runs with `trust: false`. Raw LaTeX that does not parse is shown in a monospace box and reported as a warning.
- The graph `fn` is parsed by `expression.ts` (`x`, numbers, `+ - * / ^`, `sin cos tan sqrt abs exp ln log`, `pi e`, implicit multiplication). Nothing is evaluated.
- Colours must be `#hex` or a plain colour name; anything else takes the default.
- Element ids: an action's `config.id` names its element, so `FADE_OUT.elementIds` and `CLEAR_CANVAS.keepElements` can refer to it. Ids are unique per step; a repeated id gets a suffix.
- Equations from `step.equations` stack in one column at x 500 (the panel), starting at the first equation's y. Their own `x` is not used. Each is scaled to fit the 300 px panel.
- Spec gives no shapes for some config: `DRAW_FREE_BODY.forces[]` is `{angle, magnitude, label, color}`; `DRAW_CIRCUIT.components[]` is `{type: RESISTOR | CAPACITOR | BATTERY | WIRE, from, to, label}`; `DRAW_GRAPH.box` is `{x, y, width, height}` (default: left half, `x 40 y 40 w 440 h 400`); `DRAW_CIRCLE.fill` and `DRAW_PARABOLA.dashed` are booleans. See BUILD-DECISIONS.md.
- Testing: jsdom has no 2D canvas, so specs replace `konva` with `testing/konva-fake.ts`. The fake records nothing visual; the drawing itself is only checked by eye in a browser (see BUILD-DECISIONS.md, "Needs the user").

## Status
- [x] components and services (renderer, animation engine, equation renderer, scene, drawers for all 18 action types of spec 3.3)
- [x] defensive rendering: unknown types, missing or invalid fields, NaN, out-of-canvas points, bad LaTeX and bad graph functions are skipped or fall back, each with a warning
- [x] unit tests mirrored under `frontend/src/app/` (spec 3.2 sample plays to the end; malformed step plays to the end)
- [ ] no route: the lesson player (row 17) hosts this component
- [ ] visual check in a browser: not done in the build (no dev server left running)
