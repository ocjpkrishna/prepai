import { Pt } from '../../../core/models/whiteboard.model';
import { Scene, SceneItem, ShapeItem } from './scene.model';

export function clamp01(v: number): number {
	return Math.min(1, Math.max(0, v));
}

export function progress(item: { start: number; dur: number }, t: number): number {
	return item.dur <= 0 ? (t >= item.start ? 1 : 0) : clamp01((t - item.start) / item.dur);
}

export function guidePoint(item: ShapeItem, p: number): Pt {
	const g = item.guide;
	if (g.length === 0) {
		return { x: 0, y: 0 };
	}
	const next = g.findIndex((pt) => pt.at >= p);
	if (next <= 0) {
		return { x: g[Math.max(0, next)].x, y: g[Math.max(0, next)].y };
	}
	const a = g[next - 1];
	const b = g[next];
	const k = b.at === a.at ? 0 : (p - a.at) / (b.at - a.at);
	return { x: a.x + (b.x - a.x) * k, y: a.y + (b.y - a.y) * k };
}

export function cursorFor(item: SceneItem, t: number): Pt {
	const p = progress(item, t);
	if (item.type === 'shape') {
		return guidePoint(item, p);
	}
	if (item.type === 'highlight') {
		return { x: item.x + item.w * p, y: item.y + item.h };
	}
	return { x: item.x + item.w * p, y: item.y + item.h * 0.85 };
}

export interface Cursor extends Pt {
	section: number;
	layer: 'fig' | 'col';
}

export function cursorAt(scene: Scene, t: number): Cursor | null {
	let last: SceneItem | null = null;
	for (const item of scene.items) {
		const drawable = item.type !== 'highlight' || item.w > 0;
		if (drawable && item.start <= t && (last === null || item.start >= last.start)) {
			last = item;
		}
	}
	return last ? { ...cursorFor(last, t), section: last.section, layer: last.layer } : null;
}

export function stepIndexAt(scene: Scene, t: number): number {
	const i = scene.steps.findIndex((s) => t < s.end);
	return i < 0 ? scene.steps.length - 1 : i;
}

export function wordIndexAt(scene: Scene, t: number): number {
	const step = scene.steps[stepIndexAt(scene, t)];
	if (!step) {
		return -1;
	}
	const k = clamp01((t - step.start) / step.narrationDur);
	return Math.min(step.words.length - 1, Math.floor(k * step.words.length));
}

export function fadeOut(item: SceneItem, t: number, fadeSeconds = 0.4): number {
	return item.erasedAt === null || t < item.erasedAt ? 1 : 1 - clamp01((t - item.erasedAt) / fadeSeconds);
}
