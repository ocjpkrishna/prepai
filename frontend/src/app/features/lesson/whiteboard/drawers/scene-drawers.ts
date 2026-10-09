import { Line, Rect } from '../konva-shapes';
import { Point } from '../../../../core/models/canvas-action.model';
import { ActionConfig } from '../action-config';
import { DrawContext, Drawer } from '../draw-context';
import { ExpressionFn, compileExpression } from '../expression';
import { clampToCanvas, lerpPoint, midpoint, polar, stagger } from '../geometry';
import {
  BATTERY_LONG_HALF,
  BATTERY_SHORT_HALF,
  BODY_SIZE,
  CAPACITOR_GAP,
  CAPACITOR_PLATE_HALF,
  DEFAULT_GRAPH_BOX,
  DEFAULT_X_RANGE,
  DEFAULT_Y_RANGE,
  GRAPH_SAMPLES,
  HIGHLIGHT_OPACITY,
  MAX_MAGNITUDE,
  MIN_REGION_POINTS,
  PX_PER_UNIT,
  RESISTOR_LENGTH,
  RESISTOR_THICKNESS,
  STROKE_WIDTH,
} from '../whiteboard.constants';
import { addLabel, addShape, addStroke, offsetPoint } from './drawing-helpers';

const HALF = 0.5;
const DEGREES_PER_RADIAN = 180 / Math.PI;

export const drawFadeOut: Drawer = (context, config, progress) => {
  const ids = config.strings('elementIds');
  context.scene.fade(ids, 1 - progress);
  if (progress >= 1) {
    context.scene.remove(ids);
  }
  return null;
};

export const drawClearCanvas: Drawer = (context, config) => {
  context.scene.clearExcept(config.strings('keepElements'));
  return null;
};

/** A body square with its forces drawn one after another, each as an arrow from the centre. */
export const drawFreeBody: Drawer = (context, config, progress) => {
  const center = config.point('center');
  const forces = config.list('forces');
  const half = BODY_SIZE * HALF;
  addShape(context, new Rect({
    x: center.x - half,
    y: center.y - half,
    width: BODY_SIZE,
    height: BODY_SIZE,
    stroke: context.palette.ink,
    strokeWidth: STROKE_WIDTH,
    opacity: progress,
  }));
  let tip: Point = center;
  forces.forEach((force, index) => {
    const share = stagger(progress, index, forces.length);
    const end = drawForce(context, center, force, share);
    if (share > 0 && share < 1) {
      tip = end;
    }
  });
  return tip;
};

export const drawCircuit: Drawer = (context, config, progress) => {
  const parts = config.list('components');
  let tip: Point | null = null;
  parts.forEach((part, index) => {
    const share = stagger(progress, index, parts.length);
    const end = drawComponent(context, part, share);
    if (share > 0 && share < 1) {
      tip = end;
    }
  });
  return tip;
};

/** A function graph in a box on the left of the canvas, traced from the left edge to `progress`. */
export const drawGraph: Drawer = (context, config, progress) => {
  const evaluate = compileExpression(config.text('fn'));
  const xRange = config.range('xRange', DEFAULT_X_RANGE);
  const yRange = config.range('yRange', DEFAULT_Y_RANGE);
  const toCanvas = graphMapping(readBox(config), xRange, yRange);
  const color = config.color('color', context.palette.ink);
  if (progress > 0) {
    drawGraphAxes(context, xRange, yRange, toCanvas);
  }
  const segments = sampleSegments(evaluate, xRange, yRange, progress, toCanvas);
  segments.forEach((segment) => addStroke(context, segment, { color }));
  return segments.at(-1)?.at(-1) ?? null;
};

export const drawHighlight: Drawer = (context, config, progress) => {
  const points = config.points('points', MIN_REGION_POINTS);
  const color = config.color('color', context.palette.ink);
  const opacity = config.number('opacity', { fallback: HIGHLIGHT_OPACITY, min: 0, max: 1 });
  addShape(context, new Line({ points: points.flatMap((point) => [point.x, point.y]), closed: true, fill: color, opacity: opacity * progress }));
  return null;
};

function drawForce(context: DrawContext, center: Point, force: ActionConfig, share: number): Point {
  const angle = force.number('angle', { fallback: 0 });
  const magnitude = force.number('magnitude', { fallback: 1, min: 0, max: MAX_MAGNITUDE });
  const color = force.color('color', context.palette.ink);
  const end = clampToCanvas(polar(center, magnitude * PX_PER_UNIT, angle));
  const tip = lerpPoint(center, end, share);
  addStroke(context, [center, tip], { color, heads: 'end' });
  addLabel(context, force.text('label', ''), end, color, share);
  return tip;
}

function drawComponent(context: DrawContext, part: ActionConfig, share: number): Point {
  const from = part.point('from');
  const to = part.point('to');
  const ink = context.palette.ink;
  const end = lerpPoint(from, to, share);
  addStroke(context, [from, end], { color: ink });
  if (share >= 1) {
    drawSymbol(context, part.text('type', 'WIRE'), from, to);
  }
  addLabel(context, part.text('label', ''), midpoint(from, to), ink, share);
  return end;
}

/** Symbols for the common parts. Any other type is a plain wire. */
function drawSymbol(context: DrawContext, type: string, from: Point, to: Point): void {
  const mid = midpoint(from, to);
  const angle = Math.atan2(to.y - from.y, to.x - from.x);
  const along = { x: Math.cos(angle), y: Math.sin(angle) };
  const across = { x: -along.y, y: along.x };
  if (type === 'RESISTOR') {
    addShape(context, new Rect({
      x: mid.x,
      y: mid.y,
      offsetX: RESISTOR_LENGTH * HALF,
      offsetY: RESISTOR_THICKNESS * HALF,
      width: RESISTOR_LENGTH,
      height: RESISTOR_THICKNESS,
      rotation: angle * DEGREES_PER_RADIAN,
      stroke: context.palette.ink,
      strokeWidth: STROKE_WIDTH,
    }));
  } else if (type === 'CAPACITOR') {
    plate(context, offsetPoint(mid, along, -CAPACITOR_GAP), across, CAPACITOR_PLATE_HALF);
    plate(context, offsetPoint(mid, along, CAPACITOR_GAP), across, CAPACITOR_PLATE_HALF);
  } else if (type === 'BATTERY') {
    plate(context, offsetPoint(mid, along, -CAPACITOR_GAP), across, BATTERY_SHORT_HALF);
    plate(context, offsetPoint(mid, along, CAPACITOR_GAP), across, BATTERY_LONG_HALF);
  }
}

function plate(context: DrawContext, centre: Point, across: Point, half: number): void {
  addStroke(context, [offsetPoint(centre, across, -half), offsetPoint(centre, across, half)], { color: context.palette.ink });
}

interface Box {
  x: number;
  y: number;
  width: number;
  height: number;
}

function readBox(config: ActionConfig): Box {
  const box = config.child('box') ?? new ActionConfig({}, config.notes);
  return {
    x: box.number('x', { fallback: DEFAULT_GRAPH_BOX.x }),
    y: box.number('y', { fallback: DEFAULT_GRAPH_BOX.y }),
    width: box.number('width', { fallback: DEFAULT_GRAPH_BOX.width, min: 1 }),
    height: box.number('height', { fallback: DEFAULT_GRAPH_BOX.height, min: 1 }),
  };
}

function graphMapping(box: Box, xRange: [number, number], yRange: [number, number]): (x: number, y: number) => Point {
  const [xMin, xMax] = xRange;
  const [yMin, yMax] = yRange;
  return (x, y) => ({
    x: box.x + ((x - xMin) / (xMax - xMin)) * box.width,
    y: box.y + box.height - ((y - yMin) / (yMax - yMin)) * box.height,
  });
}

function drawGraphAxes(
  context: DrawContext,
  xRange: [number, number],
  yRange: [number, number],
  toCanvas: (x: number, y: number) => Point,
): void {
  const [xMin, xMax] = xRange;
  const [yMin, yMax] = yRange;
  const axisStyle = { color: context.palette.muted };
  if (yMin <= 0 && yMax >= 0) {
    addStroke(context, [toCanvas(xMin, 0), toCanvas(xMax, 0)], axisStyle);
  }
  if (xMin <= 0 && xMax >= 0) {
    addStroke(context, [toCanvas(0, yMin), toCanvas(0, yMax)], axisStyle);
  }
}

/** Samples the curve; a point outside the y range or a non-finite value breaks the line. */
function sampleSegments(
  evaluate: ExpressionFn,
  xRange: [number, number],
  yRange: [number, number],
  progress: number,
  toCanvas: (x: number, y: number) => Point,
): Point[][] {
  const [xMin, xMax] = xRange;
  const [yMin, yMax] = yRange;
  const segments: Point[][] = [];
  let current: Point[] = [];
  const count = Math.round(GRAPH_SAMPLES * progress);
  for (let index = 0; index <= count; index += 1) {
    const x = xMin + ((xMax - xMin) * index) / GRAPH_SAMPLES;
    const y = evaluate(x);
    if (Number.isFinite(y) && y >= yMin && y <= yMax) {
      current.push(toCanvas(x, y));
    } else if (current.length > 0) {
      segments.push(current);
      current = [];
    }
  }
  if (current.length > 0) {
    segments.push(current);
  }
  return segments;
}
