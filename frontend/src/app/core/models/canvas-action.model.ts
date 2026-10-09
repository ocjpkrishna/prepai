/** A point in canvas units. The whiteboard is 800 by 500 (spec 3.3). */
export interface Point {
  x: number;
  y: number;
}

/** Action types from spec 3.3. */
export const CANVAS_ACTION_TYPES = [
  'DRAW_AXIS',
  'DRAW_ARROW',
  'DRAW_DASHED_LINE',
  'DRAW_LINE',
  'DRAW_ARC',
  'DRAW_PARABOLA',
  'DRAW_CIRCLE',
  'DRAW_POINT',
  'DRAW_DOUBLE_ARROW',
  'WRITE_TEXT',
  'WRITE_LATEX',
  'DRAW_VECTOR',
  'DRAW_FREE_BODY',
  'DRAW_CIRCUIT',
  'DRAW_GRAPH',
  'HIGHLIGHT_REGION',
  'CLEAR_CANVAS',
  'FADE_OUT',
] as const;

export type CanvasActionType = (typeof CANVAS_ACTION_TYPES)[number];

/**
 * The type is a plain string on purpose: the server may send a type this client does not know,
 * and the renderer skips it instead of failing the whole lesson.
 */
export interface CanvasAction {
  type: string;
  config: Record<string, unknown>;
  animationDuration?: number;
}

export function isKnownActionType(type: string): type is CanvasActionType {
  return (CANVAS_ACTION_TYPES as readonly string[]).includes(type);
}
