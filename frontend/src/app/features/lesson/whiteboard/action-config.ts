import { Point } from '../../../core/models/canvas-action.model';
import { clamp, clampToCanvas } from './geometry';
import { MAX_LIST_ITEMS, TEXT_LIMIT } from './whiteboard.constants';

export type RawRecord = Record<string, unknown>;

export interface NumberOptions {
  fallback?: number;
  min?: number;
  max?: number;
}

/** Thrown when an action lacks a field it cannot draw without. The renderer skips that action. */
export class ActionConfigError extends Error {}

const COLOR_PATTERN = /^(#[0-9a-f]{3,8}|[a-z]{3,20})$/i;

export function isRecord(value: unknown): value is RawRecord {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

export function isFiniteNumber(value: unknown): value is number {
  return typeof value === 'number' && Number.isFinite(value);
}

function toPoint(value: unknown): Point | undefined {
  if (!isRecord(value) || !isFiniteNumber(value['x']) || !isFiniteNumber(value['y'])) {
    return undefined;
  }
  return { x: value['x'], y: value['y'] };
}

/**
 * Reads one action's config without ever throwing on bad data. A missing or invalid field
 * takes its fallback, or throws ActionConfigError when there is none. Every fallback and
 * clamp is recorded in `notes`, which the renderer turns into warnings.
 */
export class ActionConfig {
  constructor(private readonly raw: RawRecord, readonly notes: string[] = []) {}

  number(key: string, options: NumberOptions = {}): number {
    const { fallback, min = -Infinity, max = Infinity } = options;
    const value = this.read(key, (candidate) => (isFiniteNumber(candidate) ? candidate : undefined), fallback);
    const clamped = clamp(value, min, max);
    if (clamped !== value) {
      this.notes.push(`${key} is outside its range, clamped`);
    }
    return clamped;
  }

  point(key: string, fallback?: Point): Point {
    return this.clampPoint(key, this.read(key, toPoint, fallback));
  }

  points(key: string, minCount: number): Point[] {
    return this.items(key, toPoint, minCount).map((point) => this.clampPoint(key, point));
  }

  text(key: string, fallback?: string): string {
    return this.read(key, (candidate) => (typeof candidate === 'string' ? candidate.slice(0, TEXT_LIMIT) : undefined), fallback);
  }

  color(key: string, fallback: string): string {
    return this.read(key, (candidate) => (typeof candidate === 'string' && COLOR_PATTERN.test(candidate) ? candidate : undefined), fallback);
  }

  flag(key: string, fallback: boolean): boolean {
    return this.read(key, (candidate) => (typeof candidate === 'boolean' ? candidate : undefined), fallback);
  }

  range(key: string, fallback: [number, number]): [number, number] {
    return this.read(key, (candidate) => this.toRange(candidate), fallback);
  }

  strings(key: string): string[] {
    return this.items(key, (candidate) => (typeof candidate === 'string' ? candidate : undefined), 0);
  }

  list(key: string, minCount = 0): ActionConfig[] {
    return this.items(key, (candidate) => (isRecord(candidate) ? new ActionConfig(candidate, this.notes) : undefined), minCount);
  }

  child(key: string): ActionConfig | undefined {
    const value = this.raw[key];
    return isRecord(value) ? new ActionConfig(value, this.notes) : undefined;
  }

  private read<T>(key: string, parse: (value: unknown) => T | undefined, fallback?: T): T {
    const value = this.raw[key];
    const parsed = value === undefined ? undefined : parse(value);
    if (parsed !== undefined) {
      return parsed;
    }
    if (fallback === undefined) {
      throw new ActionConfigError(`${key} is missing or invalid`);
    }
    if (value !== undefined) {
      this.notes.push(`${key} is invalid, default used`);
    }
    return fallback;
  }

  private items<T>(key: string, parse: (value: unknown) => T | undefined, minCount: number): T[] {
    const value = this.raw[key];
    const entries = Array.isArray(value) ? value.slice(0, MAX_LIST_ITEMS) : [];
    const parsed = entries.map(parse).filter((item): item is T => item !== undefined);
    if (parsed.length < entries.length) {
      this.notes.push(`${key} has ${entries.length - parsed.length} invalid entries, skipped`);
    }
    if (parsed.length < minCount) {
      throw new ActionConfigError(`${key} needs at least ${minCount} valid entries`);
    }
    return parsed;
  }

  private clampPoint(key: string, point: Point): Point {
    const clamped = clampToCanvas(point);
    if (clamped.x !== point.x || clamped.y !== point.y) {
      this.notes.push(`${key} is outside the canvas, clamped`);
    }
    return clamped;
  }

  private toRange(value: unknown): [number, number] | undefined {
    if (!Array.isArray(value) || value.length !== 2) {
      return undefined;
    }
    const [low, high] = value as unknown[];
    return isFiniteNumber(low) && isFiniteNumber(high) && low < high ? [low, high] : undefined;
  }
}
