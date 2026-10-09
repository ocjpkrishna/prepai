import { Circle, Line } from '../konva-shapes';
import { arcPoints, clampToCanvas, flatten, lerpPoint, midpoint, polar, quadraticPoints, stagger } from '../geometry';
import { Drawer } from '../draw-context';
import {
  ARC_SEGMENTS,
  CANVAS_WIDTH,
  CIRCLE_SEGMENTS,
  DASH_PATTERN,
  DEFAULT_POINT_RADIUS,
  FULL_TURN_DEGREES,
  HIGHLIGHT_OPACITY,
  MAX_MAGNITUDE,
  MAX_POINT_RADIUS,
  MAX_STROKE_WIDTH,
  MIN_STROKE_WIDTH,
  PARABOLA_SEGMENTS,
  PX_PER_UNIT,
  STROKE_WIDTH,
} from '../whiteboard.constants';
import { addLabel, addShape, addStroke } from './drawing-helpers';

const HALF = 0.5;

export const drawAxis: Drawer = (context, config, progress) => {
  const origin = config.point('origin');
  const xEnd = { x: origin.x + config.number('xLength'), y: origin.y };
  const yEnd = { x: origin.x, y: origin.y - config.number('yLength') };
  const ink = context.palette.ink;
  const xProgress = stagger(progress, 0, 2);
  const yProgress = stagger(progress, 1, 2);
  const xTip = lerpPoint(origin, xEnd, xProgress);
  const yTip = lerpPoint(origin, yEnd, yProgress);
  addStroke(context, [origin, xTip], { color: ink, heads: 'end' });
  addStroke(context, [origin, yTip], { color: ink, heads: 'end' });
  addLabel(context, config.text('xLabel', ''), xEnd, ink, xProgress);
  addLabel(context, config.text('yLabel', ''), yEnd, ink, yProgress);
  return yProgress > 0 ? yTip : xTip;
};

export const drawArrow: Drawer = (context, config, progress) => {
  const from = config.point('from');
  const to = config.point('to');
  const color = config.color('color', context.palette.ink);
  const tip = lerpPoint(from, to, progress);
  addStroke(context, [from, tip], { color, heads: 'end' });
  addLabel(context, config.text('label', ''), midpoint(from, to), color, progress);
  return tip;
};

export const drawDashedLine: Drawer = (context, config, progress) => {
  const from = config.point('from');
  const to = config.point('to');
  const color = config.color('color', context.palette.ink);
  const tip = lerpPoint(from, to, progress);
  addStroke(context, [from, tip], { color, dash: DASH_PATTERN });
  addLabel(context, config.text('label', ''), midpoint(from, to), color, progress);
  return tip;
};

export const drawLine: Drawer = (context, config, progress) => {
  const from = config.point('from');
  const to = config.point('to');
  const color = config.color('color', context.palette.ink);
  const strokeWidth = config.number('strokeWidth', { fallback: STROKE_WIDTH, min: MIN_STROKE_WIDTH, max: MAX_STROKE_WIDTH });
  const tip = lerpPoint(from, to, progress);
  addStroke(context, [from, tip], { color, strokeWidth });
  return tip;
};

export const drawDoubleArrow: Drawer = (context, config, progress) => {
  const from = config.point('from');
  const to = config.point('to');
  const color = config.color('color', context.palette.ink);
  const tip = lerpPoint(from, to, progress);
  addStroke(context, [from, tip], { color, heads: 'both' });
  addLabel(context, config.text('label', ''), midpoint(from, to), color, progress);
  return tip;
};

export const drawArc: Drawer = (context, config, progress) => {
  const center = config.point('center');
  const radius = config.number('radius', { min: 0, max: CANVAS_WIDTH });
  const start = config.number('startAngle', { fallback: 0 });
  const end = config.number('endAngle', { fallback: start });
  const color = config.color('color', context.palette.ink);
  const sweep = (end - start) * progress;
  const points = arcPoints(center, radius, start, sweep, ARC_SEGMENTS);
  addStroke(context, points, { color });
  addLabel(context, config.text('label', ''), polar(center, radius, start + sweep * HALF), color, progress);
  return points[points.length - 1];
};

/** A parabola through start and end whose apex is `peak`: the control point is chosen so the curve passes through it. */
export const drawParabola: Drawer = (context, config, progress) => {
  const start = config.point('start');
  const peak = config.point('peak');
  const end = config.point('end');
  const color = config.color('color', context.palette.ink);
  const dashed = config.flag('dashed', false);
  const control = {
    x: 2 * peak.x - (start.x + end.x) * HALF,
    y: 2 * peak.y - (start.y + end.y) * HALF,
  };
  const points = quadraticPoints(start, control, end, progress, PARABOLA_SEGMENTS);
  addStroke(context, points, { color, dash: dashed ? DASH_PATTERN : undefined });
  return points[points.length - 1];
};

/** A circle is traced round its edge; a filled circle gets its fill once the trace completes. */
export const drawCircle: Drawer = (context, config, progress) => {
  const center = config.point('center');
  const radius = config.number('radius', { min: 0, max: CANVAS_WIDTH });
  const color = config.color('color', context.palette.ink);
  const filled = config.flag('fill', false);
  const points = arcPoints(center, radius, 0, FULL_TURN_DEGREES * progress, CIRCLE_SEGMENTS);
  addStroke(context, points, { color });
  if (filled && progress >= 1) {
    addShape(context, new Line({ points: flatten(points), closed: true, fill: color, opacity: HIGHLIGHT_OPACITY }));
  }
  return points[points.length - 1];
};

export const drawPoint: Drawer = (context, config, progress) => {
  const position = config.point('position');
  const color = config.color('color', context.palette.ink);
  const radius = config.number('radius', { fallback: DEFAULT_POINT_RADIUS, min: 0, max: MAX_POINT_RADIUS });
  addShape(context, new Circle({ x: position.x, y: position.y, radius: radius * progress, fill: color }));
  addLabel(context, config.text('label', ''), position, color, progress);
  return position;
};

/** A physics vector: its length is magnitude times PX_PER_UNIT, its direction is the maths angle. */
export const drawVector: Drawer = (context, config, progress) => {
  const origin = config.point('origin');
  const magnitude = config.number('magnitude', { fallback: 0, min: 0, max: MAX_MAGNITUDE });
  const angle = config.number('angle', { fallback: 0 });
  const color = config.color('color', context.palette.ink);
  const end = clampToCanvas(polar(origin, magnitude * PX_PER_UNIT, angle));
  const tip = lerpPoint(origin, end, progress);
  addStroke(context, [origin, tip], { color, heads: 'end' });
  addLabel(context, config.text('label', ''), end, color, progress);
  return tip;
};
