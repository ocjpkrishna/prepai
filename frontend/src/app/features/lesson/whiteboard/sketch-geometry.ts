import { Pt, SketchParams } from '../../../core/models/whiteboard.model';
import { Rect } from './scene.model';

export type XY = [number, number];

export interface Stroke {
	kind: 'poly' | 'curve' | 'polygon' | 'ellipse';
	pts: XY[];
	color: string;
	dashed?: boolean;
	fill?: string;
}

export interface LabelReq {
	text: string;
	at: Pt;
	prefer: 'above' | 'below' | 'left' | 'right' | 'center';
	size?: number;
	color?: string;
	fixed?: boolean;
}

export interface Built {
	strokes: Stroke[];
	labels: LabelReq[];
	anchors: Record<string, Pt>;
}

export interface Entity {
	ref: string;
	kind: string;
	anchors: Record<string, Pt>;
	bbox: Rect;
	frame: Frame | null;
	itemIds: string[];
	section: number;
	layer: 'fig' | 'col';
}

export interface Frame {
	id: number;
	x: number;
	y: number;
	w: number;
	h: number;
	groundY: number;
	slot: number;
	lanes: Record<string, number>;
	axes?: Entity;
	lastArrow?: Entity;
}

export interface BuildCtx {
	frame: Frame;
	resolve(spec: string | undefined): Pt | undefined;
	entity(ref: string | undefined): Entity | undefined;
}

export const FRAME_W = 520;
export const FRAME_H = 440;
const HEAD = 14;
const LANE_GAP = 48;
const BRACKET_OFFSET = 44;
const SLOT_COLS = 2;
const SLOT_W = 230;
const SLOT_H = 190;
const LABEL_SIZE = 20;

export function newFrame(id: number, x: number, y: number): Frame {
	return { id, x, y, w: FRAME_W, h: FRAME_H, groundY: y + FRAME_H - 120, slot: 0, lanes: {} };
}

export function slotCenter(frame: Frame): Pt {
	const i = frame.slot++;
	return { x: frame.x + 170 + (i % SLOT_COLS) * SLOT_W, y: frame.y + 130 + Math.floor(i / SLOT_COLS) * SLOT_H };
}

const xy = (p: Pt): XY => [p.x, p.y];
const mid = (a: Pt, b: Pt): Pt => ({ x: (a.x + b.x) / 2, y: (a.y + b.y) / 2 });
const color = (p: SketchParams, fallback = 'ink'): string => p.color ?? fallback;

export function arrowHead(tip: Pt, from: Pt, c: string): Stroke {
	const dx = tip.x - from.x;
	const dy = tip.y - from.y;
	const len = Math.hypot(dx, dy) || 1;
	const ux = dx / len;
	const uy = dy / len;
	const bx = tip.x - ux * HEAD;
	const by = tip.y - uy * HEAD;
	return { kind: 'poly', color: c, pts: [[bx - uy * 6, by + ux * 6], xy(tip), [bx + uy * 6, by - ux * 6]] };
}

export function segment(a: Pt, b: Pt, c: string, dashed = false): Stroke {
	return { kind: 'poly', color: c, pts: [xy(a), xy(b)], dashed };
}

export function arrow(a: Pt, b: Pt, c: string): Stroke[] {
	return [segment(a, b, c), arrowHead(b, a, c)];
}

export function bboxOf(strokes: Stroke[]): Rect {
	const pts = strokes.flatMap((s) => (s.kind === 'ellipse' ? ellipseCorners(s) : s.pts));
	const xs = pts.map((p) => p[0]);
	const ys = pts.map((p) => p[1]);
	const x = Math.min(...xs);
	const y = Math.min(...ys);
	return { x, y, w: Math.max(...xs) - x, h: Math.max(...ys) - y };
}

function ellipseCorners(s: Stroke): XY[] {
	const [c, size] = s.pts;
	return [[c[0] - size[0] / 2, c[1] - size[1] / 2], [c[0] + size[0] / 2, c[1] + size[1] / 2]];
}

export function baselineOf(entity: Entity): number {
	return entity.anchors['origin']?.y ?? entity.anchors['from']?.y ?? entity.bbox.y + entity.bbox.h;
}

export function buildSketch(ctx: BuildCtx, kind: string, p: SketchParams): Built | null {
	switch (kind) {
		case 'axes': return axes(ctx, p);
		case 'arrow': return arrowSketch(ctx, p);
		case 'line': return lineSketch(ctx, p);
		case 'parabola':
		case 'curve': return curveSketch(ctx, p);
		case 'circle': return circle(ctx, p);
		case 'box': return box(ctx, p);
		case 'bracket': return bracket(ctx, p);
		case 'triangle':
		case 'right_triangle':
		case 'square':
		case 'pentagon':
		case 'hexagon': return polygonSketch(ctx, kind, p);
		case 'number_line': return numberLine(ctx, p);
		case 'sine_wave':
		case 'square_wave':
		case 'sawtooth': return wave(ctx, kind, p);
		case 'table': return table(ctx, p);
		case 'freeform': return freeform(ctx, p);
		default: return null;
	}
}

function labelOf(p: SketchParams, at: Pt, prefer: LabelReq['prefer']): LabelReq[] {
	return p.label ? [{ text: p.label, at, prefer, color: color(p) }] : [];
}

function axes(ctx: BuildCtx, p: SketchParams): Built {
	const f = ctx.frame;
	const o = { x: f.x + 70, y: f.groundY };
	const xEnd = { x: f.x + f.w - 30, y: o.y };
	const yEnd = { x: o.x, y: f.y + 30 };
	const strokes = [...arrow(o, xEnd, 'ink'), ...arrow(o, yEnd, 'ink')];
	const labels: LabelReq[] = [];
	if (p.xLabel) {
		labels.push({ text: p.xLabel, at: { x: xEnd.x - 36, y: o.y + 6 }, prefer: 'below' });
	}
	if (p.yLabel) {
		labels.push({ text: p.yLabel, at: { x: o.x + 6, y: yEnd.y }, prefer: 'right' });
	}
	return { strokes, labels, anchors: { origin: o, x: xEnd, y: yEnd } };
}

function arrowSketch(ctx: BuildCtx, p: SketchParams): Built {
	const f = ctx.frame;
	const len = p.length ?? (f.axes ? 230 : 150);
	const ang = ((p.angle ?? (f.axes ? 45 : 0)) * Math.PI) / 180;
	const slot = p.from || f.axes ? undefined : slotCenter(f);
	const from = ctx.resolve(p.from) ?? f.axes?.anchors['origin'] ?? { x: slot!.x - len / 2, y: slot!.y };
	const to = ctx.resolve(p.to) ?? { x: from.x + len * Math.cos(ang), y: from.y - len * Math.sin(ang) };
	const c = color(p, 'blue');
	const deg = p.angle ?? (f.axes ? 45 : 0);
	const along = { x: from.x + (to.x - from.x) * 0.65, y: from.y + (to.y - from.y) * 0.65 };
	return {
		strokes: arrow(from, to, c),
		labels: labelOf(p, along, deg > 15 && deg < 165 ? 'left' : 'above'),
		anchors: { from, to, mid: mid(from, to) },
	};
}

function lineEnds(ctx: BuildCtx, p: SketchParams): [Pt, Pt] {
	const f = ctx.frame;
	const arrowEntity = f.lastArrow;
	if (p.ground) {
		return [{ x: f.x + 30, y: f.groundY }, { x: f.x + f.w - 30, y: f.groundY }];
	}
	if (p.along && arrowEntity) {
		const a = arrowEntity.anchors;
		const corner = p.along === 'x' ? { x: a['to'].x, y: a['from'].y } : { x: a['to'].x, y: a['from'].y };
		return p.along === 'x' ? [a['from'], corner] : [corner, a['to']];
	}
	const from = ctx.resolve(p.from) ?? slotCenter(f);
	const dropEntity = ctx.entity(p.dropTo);
	if (dropEntity) {
		return [from, { x: from.x, y: baselineOf(dropEntity) }];
	}
	return [from, ctx.resolve(p.to) ?? { x: from.x + 120, y: from.y }];
}

function lineSketch(ctx: BuildCtx, p: SketchParams): Built {
	const [from, to] = lineEnds(ctx, p);
	const prefer = p.along === 'y' || to.x === from.x ? 'right' : p.along === 'x' ? 'below' : 'above';
	return {
		strokes: [segment(from, to, color(p), p.dashed ?? Boolean(p.along || p.dropTo))],
		labels: labelOf(p, mid(from, to), prefer),
		anchors: { from, to, mid: mid(from, to) },
	};
}

function curveSketch(ctx: BuildCtx, p: SketchParams): Built {
	const f = ctx.frame;
	const slot = p.from || f.axes ? undefined : slotCenter(f);
	const start = ctx.resolve(p.from) ?? f.axes?.anchors['origin'] ?? { x: slot!.x - 150, y: slot!.y + 40 };
	const end = ctx.resolve(p.to) ?? f.axes?.anchors['x'] ?? { x: start.x + 300, y: start.y };
	const apex = p.height ?? (f.axes ? (f.axes.anchors['origin'].y - f.axes.anchors['y'].y) * 0.6 : 110);
	const horizontal = p.launch === 'horizontal';
	const pts: XY[] = [];
	const steps = 16;
	for (let i = 0; i <= steps; i++) {
		const u = i / steps;
		const x = start.x + (end.x - start.x) * u;
		const base = start.y + (end.y - start.y) * (horizontal ? u * u : u);
		pts.push([x, horizontal ? base : base - 4 * apex * u * (1 - u)]);
	}
	const peak = horizontal ? start : { x: pts[steps / 2][0], y: pts[steps / 2][1] };
	return {
		strokes: [{ kind: 'curve', pts, color: color(p, 'blue') }],
		labels: labelOf(p, { x: pts[Math.round(steps * 0.7)][0], y: pts[Math.round(steps * 0.7)][1] }, 'above'),
		anchors: { start, end, peak },
	};
}

function circle(ctx: BuildCtx, p: SketchParams): Built {
	const at = ctx.resolve(p.at);
	const c = at ?? slotCenter(ctx.frame);
	const r = p.size ?? (at ? 7 : 55);
	const col = color(p, at ? 'red' : 'ink');
	const stroke: Stroke = { kind: 'ellipse', pts: [xy(c), [r * 2, r * 2]], color: col, fill: at ? col : undefined };
	return {
		strokes: [stroke],
		labels: labelOf(p, at ? { x: c.x + r + 4, y: c.y } : c, at ? 'right' : 'center'),
		anchors: {
			center: c, top: { x: c.x, y: c.y - r }, bottom: { x: c.x, y: c.y + r },
			left: { x: c.x - r, y: c.y }, right: { x: c.x + r, y: c.y },
		},
	};
}

export function rectAnchors(r: Rect): Record<string, Pt> {
	return {
		topLeft: { x: r.x, y: r.y }, topRight: { x: r.x + r.w, y: r.y },
		bottomLeft: { x: r.x, y: r.y + r.h }, bottomRight: { x: r.x + r.w, y: r.y + r.h },
		center: { x: r.x + r.w / 2, y: r.y + r.h / 2 }, top: { x: r.x + r.w / 2, y: r.y },
		bottom: { x: r.x + r.w / 2, y: r.y + r.h }, left: { x: r.x, y: r.y + r.h / 2 },
		right: { x: r.x + r.w, y: r.y + r.h / 2 },
	};
}

function rectPts(r: Rect): XY[] {
	return [[r.x, r.y], [r.x + r.w, r.y], [r.x + r.w, r.y + r.h], [r.x, r.y + r.h]];
}

function box(ctx: BuildCtx, p: SketchParams): Built {
	const f = ctx.frame;
	const w = p.width ?? 100;
	const h = p.height ?? 100;
	const base = ctx.entity(p.on);
	const slot = base ? undefined : slotCenter(f);
	const left = base ? f.x + 130 + (p.x ?? 0) * (f.w - 280) : slot!.x - w / 2;
	const top = base ? baselineOf(base) - h : slot!.y - h / 2;
	const rect = { x: left, y: top, w, h };
	return {
		strokes: [{ kind: 'polygon', pts: rectPts(rect), color: color(p) }],
		labels: labelOf(p, rectAnchors(rect)['center'], 'center'),
		anchors: rectAnchors(rect),
	};
}

function bracket(ctx: BuildCtx, p: SketchParams): Built {
	const f = ctx.frame;
	const side = p.side ?? 'below';
	const span = spanRect(ctx, p.spans ?? '');
	const lane = f.lanes[`${side}`] ?? 0;
	f.lanes[`${side}`] = lane + 1;
	const off = BRACKET_OFFSET + lane * LANE_GAP;
	const c = color(p, 'purple');
	return side === 'above' || side === 'below'
		? horizontalBracket(span, side, off, c, p)
		: verticalBracket(span, side, off, c, p);
}

function spanRect(ctx: BuildCtx, spec: string): Rect {
	const parts = spec.split(':').map((s) => ctx.resolve(s));
	if (parts.length === 2 && parts[0] && parts[1]) {
		const [a, b] = parts as [Pt, Pt];
		return { x: Math.min(a.x, b.x), y: Math.min(a.y, b.y), w: Math.abs(a.x - b.x), h: Math.abs(a.y - b.y) };
	}
	return ctx.entity(spec.split(/[.@]/)[0])?.bbox ?? { x: 0, y: 0, w: 0, h: 0 };
}

function horizontalBracket(r: Rect, side: string, off: number, c: string, p: SketchParams): Built {
	const y = side === 'below' ? r.y + r.h + off : r.y - off;
	const a = { x: r.x, y };
	const b = { x: r.x + r.w, y };
	const strokes = [
		segment(a, b, c), segment({ x: a.x, y: y - 7 }, { x: a.x, y: y + 7 }, c), segment({ x: b.x, y: y - 7 }, { x: b.x, y: y + 7 }, c),
	];
	return { strokes, labels: labelOf(p, mid(a, b), side === 'below' ? 'below' : 'above'), anchors: { from: a, to: b, mid: mid(a, b) } };
}

function verticalBracket(r: Rect, side: string, off: number, c: string, p: SketchParams): Built {
	const x = side === 'left' ? r.x - off : r.x + r.w + off;
	const a = { x, y: r.y };
	const b = { x, y: r.y + r.h };
	const strokes = [
		segment(a, b, c), segment({ x: x - 7, y: a.y }, { x: x + 7, y: a.y }, c), segment({ x: x - 7, y: b.y }, { x: x + 7, y: b.y }, c),
	];
	return { strokes, labels: labelOf(p, mid(a, b), side === 'left' ? 'left' : 'right'), anchors: { from: a, to: b, mid: mid(a, b) } };
}

function regular(c: Pt, radius: number, sides: number, rotate: number): XY[] {
	return Array.from({ length: sides }, (_, i) => {
		const a = rotate + (i * 2 * Math.PI) / sides;
		return [c.x + radius * Math.cos(a), c.y + radius * Math.sin(a)] as XY;
	});
}

function polygonPoints(kind: string, c: Pt, s: number): XY[] {
	switch (kind) {
		case 'right_triangle': return [[c.x - s * 0.55, c.y + s * 0.45], [c.x + s * 0.65, c.y + s * 0.45], [c.x - s * 0.55, c.y - s * 0.55]];
		case 'triangle': return [[c.x, c.y - s * 0.55], [c.x + s * 0.6, c.y + s * 0.45], [c.x - s * 0.6, c.y + s * 0.45]];
		case 'square': return rectPts({ x: c.x - s * 0.4, y: c.y - s * 0.4, w: s * 0.8, h: s * 0.8 });
		case 'pentagon': return regular(c, s * 0.6, 5, -Math.PI / 2);
		default: return regular(c, s * 0.6, 6, 0);
	}
}

function polygonSketch(ctx: BuildCtx, kind: string, p: SketchParams): Built {
	const c = ctx.resolve(p.at) ?? slotCenter(ctx.frame);
	const pts = polygonPoints(kind, c, p.size ?? 130);
	const col = color(p);
	const strokes: Stroke[] = [{ kind: 'polygon', pts, color: col }];
	if (kind === 'right_triangle') {
		const [a] = pts;
		strokes.push({ kind: 'poly', color: col, pts: [[a[0] + 14, a[1]], [a[0] + 14, a[1] - 14], [a[0], a[1] - 14]] });
	}
	const anchors: Record<string, Pt> = { center: c };
	pts.forEach((v, i) => (anchors[`v${i}`] = { x: v[0], y: v[1] }));
	return { strokes, labels: [...labelOf(p, c, 'center'), ...sideLabels(pts, c, p)], anchors };
}

function sideLabels(pts: XY[], c: Pt, p: SketchParams): LabelReq[] {
	return (p.sides ?? []).slice(0, pts.length).map((text, i) => {
		const a = pts[i];
		const b = pts[(i + 1) % pts.length];
		const m = { x: (a[0] + b[0]) / 2, y: (a[1] + b[1]) / 2 };
		const dx = m.x - c.x;
		const dy = m.y - c.y;
		const prefer = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 'right' : 'left') : dy > 0 ? 'below' : 'above';
		return { text, at: m, prefer, color: color(p, 'ink') } as LabelReq;
	});
}

function numberLine(ctx: BuildCtx, p: SketchParams): Built {
	const f = ctx.frame;
	const min = p.min ?? 0;
	const max = p.max ?? 10;
	const y = slotCenter(f).y;
	const x0 = f.x + 50;
	const x1 = f.x + f.w - 50;
	const at = (v: number): Pt => ({ x: x0 + ((v - min) / (max - min)) * (x1 - x0), y });
	const strokes = arrow({ x: x0 - 20, y }, { x: x1 + 20, y }, 'ink');
	const labels: LabelReq[] = [];
	const step = max - min > 12 ? 2 : 1;
	for (let v = min; v <= max; v += step) {
		strokes.push(segment({ x: at(v).x, y: y - 6 }, { x: at(v).x, y: y + 6 }, 'ink'));
		labels.push({ text: String(v), at: { x: at(v).x, y: y + 12 }, prefer: 'below', fixed: true, size: 18 });
	}
	for (const m of p.marks ?? []) {
		strokes.push({ kind: 'ellipse', pts: [xy(at(m.at)), [16, 16]], color: 'red', fill: 'red' });
		labels.push({ text: m.label ?? String(m.at), at: { x: at(m.at).x, y: y - 14 }, prefer: 'above', color: 'red', fixed: true });
	}
	return { strokes, labels, anchors: { from: at(min), to: at(max), mid: at((min + max) / 2) } };
}

function waveY(kind: string, u: number): number {
	const phase = u % 1;
	if (kind === 'sine_wave') {
		return -Math.sin(u * 2 * Math.PI);
	}
	return kind === 'square_wave' ? (phase < 0.5 ? -1 : 1) : 1 - 2 * phase;
}

function wave(ctx: BuildCtx, kind: string, p: SketchParams): Built {
	const c = slotCenter(ctx.frame);
	const w = p.width ?? 360;
	const amp = p.height ?? 55;
	const cycles = p.size ?? 2;
	const x0 = c.x - w / 2 + 40;
	const pts: XY[] = [];
	const n = kind === 'sine_wave' ? cycles * 24 : cycles * 2;
	for (let i = 0; i <= n; i++) {
		const u = (i / n) * cycles;
		const x = x0 + (i / n) * w;
		pts.push(kind === 'square_wave' && i > 0 ? [x, c.y + amp * waveY(kind, u - 1e-6)] : [x, c.y + amp * waveY(kind, u)]);
		if (kind === 'square_wave' && i < n) {
			pts.push([x, c.y + amp * waveY(kind, u + 1e-6)]);
		}
	}
	const axis = segment({ x: x0 - 10, y: c.y }, { x: x0 + w + 10, y: c.y }, 'ink', true);
	const main: Stroke = { kind: kind === 'sine_wave' ? 'curve' : 'poly', pts, color: color(p, 'blue') };
	return {
		strokes: [axis, main],
		labels: labelOf(p, { x: x0 + w / 2, y: c.y - amp - 12 }, 'above'),
		anchors: { start: { x: x0, y: c.y }, end: { x: x0 + w, y: c.y }, mid: c },
	};
}

function table(ctx: BuildCtx, p: SketchParams): Built {
	const rows = p.rows ?? [['', '']];
	const cols = Math.max(...rows.map((r) => r.length));
	const cw = 110;
	const ch = 42;
	const c = slotCenter(ctx.frame);
	const rect = { x: c.x - (cols * cw) / 2 + 60, y: c.y - (rows.length * ch) / 2, w: cols * cw, h: rows.length * ch };
	const strokes: Stroke[] = [{ kind: 'polygon', pts: rectPts(rect), color: 'ink' }];
	for (let i = 1; i < rows.length; i++) {
		strokes.push(segment({ x: rect.x, y: rect.y + i * ch }, { x: rect.x + rect.w, y: rect.y + i * ch }, 'ink'));
	}
	for (let j = 1; j < cols; j++) {
		strokes.push(segment({ x: rect.x + j * cw, y: rect.y }, { x: rect.x + j * cw, y: rect.y + rect.h }, 'ink'));
	}
	const labels = rows.flatMap((row, i) =>
		row.map((text, j) => ({
			text, at: { x: rect.x + j * cw + cw / 2, y: rect.y + i * ch + ch / 2 }, prefer: 'center', fixed: true, size: 18,
		}) as LabelReq),
	);
	return { strokes, labels, anchors: rectAnchors(rect) };
}

function freeform(ctx: BuildCtx, p: SketchParams): Built {
	const c = slotCenter(ctx.frame);
	const pts: XY[] = (p.points ?? [[0, 1], [0.5, 0], [1, 1]]).map(([u, v]) => [c.x - 150 + u * 300, c.y - 100 + v * 200] as XY);
	return {
		strokes: [{ kind: 'curve', pts, color: color(p) }],
		labels: labelOf(p, { x: c.x, y: c.y - 110 }, 'above'),
		anchors: { start: { x: pts[0][0], y: pts[0][1] }, end: { x: pts[pts.length - 1][0], y: pts[pts.length - 1][1] }, center: c },
	};
}

export { LABEL_SIZE };
