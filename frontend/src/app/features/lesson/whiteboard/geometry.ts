import { Point } from '../../../core/models/canvas-action.model';
import { CANVAS_HEIGHT, CANVAS_WIDTH } from './whiteboard.constants';

const DEGREES_TO_RADIANS = Math.PI / 180;
const HALF = 0.5;

export function clamp(value: number, min: number, max: number): number {
  return Math.min(Math.max(value, min), max);
}

export function clampToCanvas(point: Point): Point {
  return { x: clamp(point.x, 0, CANVAS_WIDTH), y: clamp(point.y, 0, CANVAS_HEIGHT) };
}

export function lerpPoint(from: Point, to: Point, progress: number): Point {
  return { x: from.x + (to.x - from.x) * progress, y: from.y + (to.y - from.y) * progress };
}

export function midpoint(from: Point, to: Point): Point {
  return lerpPoint(from, to, HALF);
}

export function flatten(points: Point[]): number[] {
  return points.flatMap((point) => [point.x, point.y]);
}

/** A point on a circle, in maths convention: angles counter-clockwise from the x axis, y pointing up. */
export function polar(center: Point, radius: number, degrees: number): Point {
  const radians = degrees * DEGREES_TO_RADIANS;
  return { x: center.x + radius * Math.cos(radians), y: center.y - radius * Math.sin(radians) };
}

export function arcPoints(center: Point, radius: number, startDegrees: number, sweepDegrees: number, steps: number): Point[] {
  return Array.from({ length: steps + 1 }, (_, index) =>
    polar(center, radius, startDegrees + (sweepDegrees * index) / steps));
}

export function quadraticPoint(start: Point, control: Point, end: Point, progress: number): Point {
  const remaining = 1 - progress;
  return {
    x: remaining * remaining * start.x + 2 * remaining * progress * control.x + progress * progress * end.x,
    y: remaining * remaining * start.y + 2 * remaining * progress * control.y + progress * progress * end.y,
  };
}

/** The part of a quadratic curve up to `progress`, sampled into `segments` steps. */
export function quadraticPoints(start: Point, control: Point, end: Point, progress: number, segments: number): Point[] {
  return Array.from({ length: segments + 1 }, (_, index) =>
    quadraticPoint(start, control, end, (progress * index) / segments));
}

/** Staggers a progress value over `count` items, so the items draw one after another. */
export function stagger(progress: number, index: number, count: number): number {
  return clamp(progress * count - index, 0, 1);
}
