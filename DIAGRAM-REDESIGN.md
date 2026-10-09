# Diagram and whiteboard redesign

Why: the first working version draws almost nothing useful. A real lesson ("Horizontal Projection from a Tower",
screenshots taken 2026-10-09) showed a single white dot and two floating equations at step 2, and one red caption at
step 3. No tower, no path, no axes, no labels. This page records what the research says and what we build instead.

## What the reference product does (dudely.co, from its public page)
- Builds every explanation "move by move on a board, never dumped as a finished image".
- Narrates in a voice and "points at the exact term it is explaining".
- Takes a typed topic, pasted notes, or a photo of a problem; accepts voice and pen questions and lets the student point at the board.
- Runs a mastery loop: the student attempts, it diagnoses the gap, re-teaches that gap, and does not advance until the student has it.
- Not public: its visual style and its technology. Our design below is inferred from the behaviour it describes, not copied.

## Why our board looks like a college project
1. The model writes raw pixel coordinates for every shape. Research on LLM diagram generation finds this the least
   reliable option (MagicGeo, PhyDrawGen). Our small model drew one dot.
2. There is no persistent scene. Steps start empty, so the tower from step 1 is gone by step 3.
3. Equations float in a side panel with no link to the drawing. Nothing is pointed at.
4. The look is flat default shapes. No ink feel, no write-on animation, no emphasis.
5. Actions fire on fixed timers, not on what the voice is saying.
6. (Fixed 2026-10-09) The narration read formulas letter by letter.

## Target design
**A. Scene templates, not coordinates.** Deterministic code draws the diagram; the model only chooses a template and its numbers.
The lesson contains `scene: { template, params, elements }`, for example
`{ template: "projectile_horizontal", params: { height: 80, speed: 15, g: 10 } }`. Templates (phase 1): horizontal and
angled projectile, free-body diagram, inclined plane, pulley and blocks, velocity-time and position-time graphs,
coordinate geometry. Phase 2: ray optics, circuits, waves, chemistry structures, titration curves.
Bonus: the template also computes the physics (time of flight, range), so the validator can check the model's final
answer against real numbers. That is an accuracy gain, not only a visual one.

**B. One persistent scene with named elements.** Each step only reveals, moves, or emphasises elements
(`reveal: ["tower","ball"]`, `emphasise: ["height-arrow"]`). Nothing disappears unless a step says so.

**C. Hand-drawn look.** Sketchy shapes with rough.js, a handwriting font for labels, and a pen cursor that draws strokes
on (stroke-dash animation). One palette, three line weights, one easing curve. Dark chalkboard and light paper themes.

**D. Narration-synchronised reveals.** The speech engine already receives narration one sentence at a time, so the
duration of each sentence is known. Each reveal and highlight carries `at: <sentence index>`. Sentence-level sync in
phase 1; word-level (forced alignment or TTS timestamps) in phase 2. Bias early: a pointer slightly ahead of the voice
reads better than one that lags.

**E. Term pointing.** Equation terms are colour-linked to diagram elements (the symbol h lights the height arrow). While
a sentence mentions a term, that term and its element glow.

**F. Mastery loop (phase 2).** Attempt, diagnose, re-teach the gap, retry. The current multiple-choice check is the first step.

**G. Fallback.** Free-form canvas actions stay for problems with no template, but are placed on a grid and anchored to
scene elements, never raw pixels.

## Build order
1. Lesson contract: `scene` with template, params, elements; steps with `reveal`, `emphasise`, `at`. Validator for the new fields.
2. Renderer: element registry, persistent scene, rough.js style, write-on animation, pen cursor.
3. Templates: horizontal projectile first (the example above), then angled projectile, free-body diagram, graphs.
4. Sync: reveal on sentence start, highlight on mention.
5. Prompt: the model picks template and params, writes narration with `at` markers.
6. Screenshot tests (Playwright) so a blank board fails the build.

## Sources
- PhyDrawGen: LLM emits a typed scene graph, a deterministic solver builds exact geometry. https://arxiv.org/pdf/2605.30512
- MagicGeo: direct coordinates from an LLM are error-prone; use constraints and a solver. https://arxiv.org/html/2502.13855v1
- Speech-synchronised whiteboard generation (element-to-transcript alignment). https://arxiv.org/pdf/2603.25870
- Narrated lecture videos with synchronised highlights (word timestamps plus placement). https://arxiv.org/pdf/2505.02966
- rough.js (sketchy shapes): https://jsr.io/@rough/roughjs/doc/~/Options.roughness; perfect-freehand (ink strokes): https://openapps.pro/packages/perfect-freehand
- JSXGraph for geometry and graphs: https://jsxgraph.uni-bayreuth.de/home/
