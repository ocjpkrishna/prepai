/**
 * The drawing contract of the lesson scene (DIAGRAM-CONTRACT.md). A template builder turns a few numbers into named
 * elements in scene space; the scene engine draws them and reveals them step by step. Nothing here knows about Konva.
 */

/** Scene space: 800 wide, 600 high, origin top-left, y grows downward. The engine scales it to the board. */
export const SCENE_WIDTH = 800;
export const SCENE_HEIGHT = 600;

export interface Pt {
  x: number;
  y: number;
}

/** Colours and weights come from the theme, never from the template: one place decides how the board looks. */
export type StyleToken = 'ink' | 'accent' | 'muted' | 'force' | 'velocity' | 'good' | 'warn';

export type TextSize = 'sm' | 'md' | 'lg';

export interface LineShape {
  kind: 'line';
  from: Pt;
  to: Pt;
  dashed?: boolean;
}

/** `double` puts a head on both ends (a measurement). */
export interface ArrowShape {
  kind: 'arrow';
  from: Pt;
  to: Pt;
  double?: boolean;
  dashed?: boolean;
}

/** A polyline the engine may smooth; used for trajectories and graph lines. */
export interface PathShape {
  kind: 'path';
  points: Pt[];
  dashed?: boolean;
}

export interface CircleShape {
  kind: 'circle';
  center: Pt;
  radius: number;
  filled?: boolean;
}

export interface PolygonShape {
  kind: 'polygon';
  points: Pt[];
  filled?: boolean;
}

/** Angles are degrees, 0 = right, counter-clockwise positive as in a maths book (the engine flips y). */
export interface ArcShape {
  kind: 'arc';
  center: Pt;
  radius: number;
  startDeg: number;
  endDeg: number;
}

export interface TextShape {
  kind: 'text';
  at: Pt;
  text: string;
  size?: TextSize;
  anchor?: 'start' | 'middle' | 'end';
}

/** KaTeX source drawn on the board (a symbol, not a whole derivation: those go to the equation panel). */
export interface LatexShape {
  kind: 'latex';
  at: Pt;
  latex: string;
  size?: TextSize;
}

export type Shape = LineShape | ArrowShape | PathShape | CircleShape | PolygonShape | ArcShape | TextShape | LatexShape;

/**
 * One named thing on the board, made of one or more shapes that appear and highlight together. The id comes from
 * scene-templates/scene-catalogue.json and is what a lesson step refers to in `reveal` and `emphasise`.
 */
export interface SceneElement {
  id: string;
  parts: Shape[];
  style?: StyleToken;
  /** Drawn behind elements with a higher layer. Ground and axes use 0, bodies 1, vectors 2, labels 3. */
  layer?: number;
  /** Shown from the start, before any step reveals it (the ground, the axes). */
  alwaysVisible?: boolean;
}

export interface BuiltScene {
  elements: SceneElement[];
  /** Numbers the template computed from its parameters (time of flight, range, ...): the catalogue lists the names. */
  derived: Record<string, number>;
}

export type TemplateBuilder = (params: Record<string, unknown>) => BuiltScene;

/** Thrown by a builder for parameters it cannot draw; the message is meant for the lesson validator. */
export class SceneParamError extends Error {}

/** What the lesson JSON carries (spec: DIAGRAM-CONTRACT.md). */
export interface LessonScene {
  template: string;
  params: Record<string, unknown>;
}

/** Points a lesson step at a scene element. `at` is the index of the narration sentence that triggers it. */
export interface SceneRef {
  element: string;
  at: number;
  /** For `emphasise`: the equation symbol this element stands for, so the symbol and the element light together. */
  term?: string;
}
