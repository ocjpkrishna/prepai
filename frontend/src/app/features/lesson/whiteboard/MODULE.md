# Whiteboard engine (frontend)

Runs a lesson's whiteboard tools (spec 3.3) on one continuous SVG notebook. Built by Agent 5. Reference for look and feel: `docs/dudely-reference.md` (user experience only, no copied code).

## Flow
`Lesson` (tools per step) -> `tool-validator.ts` (ok/error replies, spec 3.3.1) -> `board-layout.ts` (sections, anchors, label placement, timeline) -> `Scene` -> `whiteboard.component` draws it at time `t` (`playback.ts`).

## Package map
| File | Job |
|------|-----|
| `tool-validator.ts` | Pure. Checks every tool, returns `ok:` / `error:` replies and the tools that may run |
| `board-layout.ts` | Pure. Groups steps into sections (figure on the left, writing column on the right), resolves anchors (`ref`, `ref.anchor`, `ref@t`), places labels without overlaps, builds the timeline |
| `sketch-geometry.ts` | Geometry of each `sketch` kind and its named anchors |
| `diagram-geometry.ts` | `diagram` kinds (flow, tree, columns, timeline, network) and the image placeholder |
| `text-measure.ts` | KaTeX HTML and text measuring (DOM in the browser, an estimate in tests) |
| `playback.ts` | Progress, cursor position, step and word at time `t` |
| `whiteboard.component.*` | SVG rendering: rough.js paths drawn in with `stroke-dashoffset`, handwriting reveal, marker cursor, auto-scroll |

## Rules and gotchas
- Lessons carry no pixel coordinates; the layout engine places everything.
- A step joins an earlier figure when a tool names a `ref` drawn there; a step with no drawing tools joins the last figure.
- Labels are placed after every shape is known, so later shapes never run into earlier labels.
- Dashed strokes are revealed through an SVG mask (a dash array cannot also draw the stroke in).
- KaTeX and Kalam fonts must be loaded before measuring (`LessonPlayerComponent.fontsReady`).
- `move` is rejected for now; `new_page` is ignored (one continuous notebook).

## Status
- [x] Validator, layout, sketch kinds, diagram kinds, renderer, marker cursor
- [x] Unit tests: validator replies, every mock lesson has no errors, no overlapping labels, items inside the board
- [ ] Real stroke-path handwriting (text uses a handwriting font with a reveal)
- [ ] Physics and chemistry kinds (free-body, circuit, ray optics, structures)
- [ ] `move` tool, `image` generation (placeholder box only)
