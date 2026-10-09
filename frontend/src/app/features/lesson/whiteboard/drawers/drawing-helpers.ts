import { Arrow, KonvaShape, Line, Text } from '../konva-shapes';
import { Point } from '../../../../core/models/canvas-action.model';
import { DrawContext } from '../draw-context';
import { flatten } from '../geometry';
import { ARROW_HEAD_SIZE, LABEL_FONT_SIZE, LABEL_OFFSET, STROKE_WIDTH } from '../whiteboard.constants';

export interface StrokeStyle {
  color: string;
  strokeWidth?: number;
  dash?: number[];
  heads?: 'none' | 'end' | 'both';
  opacity?: number;
}

export function addShape(context: DrawContext, shape: KonvaShape): void {
  context.scene.groupFor(context.id).add(shape);
}

/** A line or arrow through `points`. Arrows need a non-zero length, so an empty arrow is skipped. */
export function addStroke(context: DrawContext, points: Point[], style: StrokeStyle): void {
  const heads = style.heads ?? 'none';
  if (points.length < 2 || (heads !== 'none' && isZeroLength(points))) {
    return;
  }
  const config = {
    points: flatten(points),
    stroke: style.color,
    fill: style.color,
    strokeWidth: style.strokeWidth ?? STROKE_WIDTH,
    dash: style.dash,
    opacity: style.opacity ?? 1,
  };
  const shape = heads === 'none'
    ? new Line(config)
    : new Arrow({
      ...config,
      pointerLength: ARROW_HEAD_SIZE,
      pointerWidth: ARROW_HEAD_SIZE,
      pointerAtBeginning: heads === 'both',
      pointerAtEnding: true,
    });
  addShape(context, shape);
}

/** A label just up and to the right of `at`. It fades in with `opacity`. */
export function addLabel(context: DrawContext, text: string, at: Point, color: string, opacity: number): void {
  if (text === '' || opacity <= 0) {
    return;
  }
  addShape(context, new Text({
    x: at.x + LABEL_OFFSET,
    y: at.y - LABEL_OFFSET,
    text,
    fontSize: LABEL_FONT_SIZE,
    fill: color,
    opacity,
  }));
}

export function offsetPoint(point: Point, direction: Point, distance: number): Point {
  return { x: point.x + direction.x * distance, y: point.y + direction.y * distance };
}

function isZeroLength(points: Point[]): boolean {
  const first = points[0];
  const last = points[points.length - 1];
  return first.x === last.x && first.y === last.y;
}
