import { CanvasActionType } from '../../../../core/models/canvas-action.model';
import { Drawer } from '../draw-context';
import { drawCircuit, drawClearCanvas, drawFadeOut, drawFreeBody, drawGraph, drawHighlight } from './scene-drawers';
import {
  drawArc,
  drawArrow,
  drawAxis,
  drawCircle,
  drawDashedLine,
  drawDoubleArrow,
  drawLine,
  drawParabola,
  drawPoint,
  drawVector,
} from './shape-drawers';
import { drawLatex, drawText } from './text-drawers';

/** One drawer per action type in spec 3.3. The record type makes a missing type a compile error. */
export const DRAWERS: Readonly<Record<CanvasActionType, Drawer>> = {
  DRAW_AXIS: drawAxis,
  DRAW_ARROW: drawArrow,
  DRAW_DASHED_LINE: drawDashedLine,
  DRAW_LINE: drawLine,
  DRAW_ARC: drawArc,
  DRAW_PARABOLA: drawParabola,
  DRAW_CIRCLE: drawCircle,
  DRAW_POINT: drawPoint,
  DRAW_DOUBLE_ARROW: drawDoubleArrow,
  WRITE_TEXT: drawText,
  WRITE_LATEX: drawLatex,
  DRAW_VECTOR: drawVector,
  DRAW_FREE_BODY: drawFreeBody,
  DRAW_CIRCUIT: drawCircuit,
  DRAW_GRAPH: drawGraph,
  HIGHLIGHT_REGION: drawHighlight,
  CLEAR_CANVAS: drawClearCanvas,
  FADE_OUT: drawFadeOut,
};
