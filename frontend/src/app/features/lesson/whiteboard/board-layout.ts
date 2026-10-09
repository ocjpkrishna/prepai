import rough from 'roughjs';
import { Lesson, LessonStep, Pt, SketchParams, ToolReply, WhiteboardTool } from '../../../core/models/whiteboard.model';
import { buildDiagram, imageBox } from './diagram-geometry';
import {
	Built, BuildCtx, Entity, Frame, LabelReq, newFrame, rectAnchors, Stroke, XY,
	arrow, bboxOf, buildSketch, FRAME_H, FRAME_W, slotCenter,
} from './sketch-geometry';
import {
	BOARD_WIDTH, GuidePoint, HighlightItem, ItemBase, LabelDraw, Layer, PathDraw, Rect, Scene, SceneItem, ShapeItem, StepTiming,
	StrokeDraw, TextItem,
} from './scene.model';
import { TextMeasurer } from './text-measure';
import { validateLesson } from './tool-validator';

const WORDS_PER_SECOND = 2.5;
const STEP_LEAD = 0.35;
const STEP_TAIL = 0.6;
const MIN_SCALE = 0.75;
const MAX_SCALE = 1.7;
const COL_GAP = 20;
const SECTION_GAP = 56;
const BOARD_TOP = 24;
const BOARD_MARGIN = 40;
const FRAME_X = 30;
const COL_X_WITH_FRAME = 590;
const COL_W_WITH_FRAME = 390;
const COL_W_FULL = 920;
const TITLE_SIZE = 28;
const TEXT_SIZE = 24;
const MATH_SIZE = 28;
const LABEL_SIZE = 20;
const MIN_FONT_SCALE = 0.4;
const HEADING_EXTRA = 6;
const MAX_CONNECT_DISTANCE = 460;
const STROKE_SPEED = 420;
const generator = rough.generator();

interface ColumnEntry {
	step: number;
	tool: number;
	kind: 'text' | 'math';
	content: string;
	size: number;
	color: string;
	heading: boolean;
	w: number;
	h: number;
	html?: string;
	x: number;
	y: number;
}

interface Section {
	colX: number;
	colW: number;
	index: number;
	steps: number[];
	hasFrame: boolean;
	top: number;
	height: number;
	frame?: Frame;
	column: ColumnEntry[];
}

interface Raw {
	item: SceneItem;
	dur: number;
}

export function buildScene(lesson: Lesson, measure: TextMeasurer): Scene {
	const validated = validateLesson(lesson);
	const sections = planSections(validated.steps);
	layoutColumns(sections, validated.steps, measure);
	const builder = new SceneBuilder(sections, validated.steps, validated.replies);
	return builder.build();
}

function refsUsed(tool: WhiteboardTool): string[] {
	const names = (spec?: string): string[] => (spec ?? '').split(':').map((s) => s.split(/[.@]/)[0].trim()).filter(Boolean);
	if (tool.tool === 'sketch') {
		const p = tool.params ?? {};
		return [p.from, p.to, p.at, p.on, p.spans, p.dropTo].flatMap(names);
	}
	if (tool.tool === 'connect') {
		return [...names(tool.from), ...names(tool.to)];
	}
	if ('target' in tool) {
		return names(tool.target);
	}
	return [];
}

function planSections(steps: LessonStep[]): Section[] {
	const sections: Section[] = [];
	const frameRefs = new Map<string, number>();
	let lastFramed: number | undefined;
	steps.forEach((step, i) => {
		const drawing = step.tools.filter((t) => t.tool === 'sketch' || t.tool === 'diagram' || t.tool === 'image');
		const explicit = drawing.length ? step.tools.flatMap(refsUsed).map((r) => frameRefs.get(r)).find((s) => s !== undefined) : undefined;
		const joined = explicit ?? (drawing.length === 0 ? lastFramed : undefined);
		const index = joined ?? sections.length;
		if (joined === undefined) {
			sections.push({ index, steps: [], hasFrame: false, top: 0, height: 0, column: [], colX: 0, colW: 0 });
		}
		sections[index].steps.push(i);
		sections[index].hasFrame ||= drawing.length > 0;
		drawing.forEach((t) => frameRefs.set((t as { ref: string }).ref, index));
		if (drawing.length > 0) {
			lastFramed = index;
		}
	});
	return sections;
}

function fit(measure: TextMeasurer, kind: 'text' | 'math', content: string, size: number, maxW: number, bold = false): { w: number; h: number; html?: string; size: number } {
	let fontSize = size;
	let m = measure(kind, content, fontSize, maxW, bold);
	if (kind === 'math' && m.w > maxW) {
		fontSize = Math.max(size * MIN_FONT_SCALE, size * (maxW / m.w));
		m = measure(kind, content, fontSize, maxW);
	}
	return { ...m, size: fontSize };
}

function layoutColumns(sections: Section[], steps: LessonStep[], measure: TextMeasurer): void {
	let top = BOARD_TOP;
	for (const section of sections) {
		const colW = section.hasFrame ? COL_W_WITH_FRAME : COL_W_FULL;
		const colX = section.hasFrame ? COL_X_WITH_FRAME : BOARD_MARGIN;
		let y = top;
		for (const si of section.steps) {
			const entries = columnEntries(steps[si], si, measure, colW);
			for (const e of entries) {
				e.x = colX;
				e.y = y;
				y += e.h + COL_GAP;
				section.column.push(e);
			}
		}
		section.top = top;
		section.colX = colX;
		section.colW = colW;
		section.height = Math.max(section.hasFrame ? FRAME_H : 0, y - top - COL_GAP);
		top += section.height + SECTION_GAP;
	}
}

function columnEntries(step: LessonStep, si: number, measure: TextMeasurer, colW: number): ColumnEntry[] {
	const out: ColumnEntry[] = [];
	const title = fit(measure, 'text', step.title, TITLE_SIZE, colW, true);
	out.push({ step: si, tool: -1, kind: 'text', content: step.title, color: 'blue', heading: true, x: 0, y: 0, ...title, h: title.h + HEADING_EXTRA });
	step.tools.forEach((tool, ti) => {
		if (tool.tool !== 'write') {
			return;
		}
		const size = tool.size ?? (tool.kind === 'math' ? MATH_SIZE : TEXT_SIZE);
		const m = fit(measure, tool.kind, tool.content, size, colW);
		out.push({ step: si, tool: ti, kind: tool.kind, content: tool.content, color: tool.color ?? 'ink', heading: false, x: 0, y: 0, ...m });
	});
	return out;
}

function lengthOf(pts: XY[]): number {
	return pts.slice(1).reduce((sum, p, i) => sum + Math.hypot(p[0] - pts[i][0], p[1] - pts[i][1]), 0);
}

function samplePoints(s: Stroke): XY[] {
	if (s.kind === 'ellipse') {
		const [c, size] = s.pts;
		return Array.from({ length: 25 }, (_, i) => {
			const a = (i / 24) * 2 * Math.PI;
			return [c[0] + (size[0] / 2) * Math.cos(a), c[1] + (size[1] / 2) * Math.sin(a)] as XY;
		});
	}
	return s.kind === 'polygon' ? [...s.pts, s.pts[0]] : s.pts;
}

function hash(text: string): number {
	return [...text].reduce((h, ch) => (h * 31 + ch.charCodeAt(0)) | 0, 7) >>> 0;
}

function roughPaths(s: Stroke, seed: number): PathDraw[] {
	const opts = { seed, roughness: 1, bowing: 1, strokeWidth: 2.6, stroke: s.color, fill: s.fill, fillStyle: 'solid' };
	const drawable = (() => {
		switch (s.kind) {
			case 'curve': return generator.curve(s.pts, opts);
			case 'polygon': return generator.polygon(s.pts, opts);
			case 'ellipse': return generator.ellipse(s.pts[0][0], s.pts[0][1], s.pts[1][0], s.pts[1][1], opts);
			default: return generator.linearPath(s.pts, opts);
		}
	})();
	return generator.toPaths(drawable).map((p) => {
		const isFill = p.stroke === 'none';
		return {
			d: p.d, color: isFill ? 'none' : s.color, width: p.strokeWidth, dash: s.dashed ? '9 8' : undefined,
			fill: isFill ? s.fill : undefined,
		};
	});
}

class SceneBuilder {
	private readonly items: SceneItem[] = [];
	private readonly entities = new Map<string, Entity>();
	private readonly obstacles = new Map<number, { segs: [XY, XY][]; rects: Rect[] }>();
	private readonly replies: ToolReply[];
	private readonly replacements = new Map<string, string[]>();
	private readonly pending: { item: ShapeItem; frame: Frame | null; reqs: LabelReq[] }[] = [];
	private counter = 0;

	constructor(private readonly sections: Section[], private readonly steps: LessonStep[], replies: ToolReply[]) {
		this.replies = [...replies];
	}

	build(): Scene {
		const timings: StepTiming[] = [];
		let clock = 0;
		this.steps.forEach((step, si) => {
			const timing = this.buildStep(step, si, clock);
			timings.push(timing);
			clock = timing.end;
		});
		this.placeAllLabels();
		const last = this.sections[this.sections.length - 1];
		const height = last ? last.top + last.height + 120 : 400;
		const sections = this.sections.map((s) => ({
			index: s.index, top: s.top, height: s.height, colX: s.colX, colW: s.colW,
			frame: s.hasFrame ? { x: FRAME_X, y: s.top, w: FRAME_W, h: FRAME_H } : null,
		}));
		return { width: BOARD_WIDTH, height, sections, items: this.items, steps: timings, duration: clock, replies: this.replies };
	}

	private placeAllLabels(): void {
		for (const p of this.pending) {
			p.item.labels = p.reqs.map((req) => this.placeLabel(p.frame, req));
		}
	}

	private sectionOf(si: number): Section {
		return this.sections.find((s) => s.steps.includes(si))!;
	}

	private frameOf(section: Section): Frame {
		section.frame ??= newFrame(section.index, FRAME_X, section.top);
		return section.frame;
	}

	private buildStep(step: LessonStep, si: number, start: number): StepTiming {
		const section = this.sectionOf(si);
		const raws: Raw[] = [];
		const title = section.column.find((e) => e.step === si && e.heading)!;
		raws.push(this.textItem(title, si));
		step.tools.forEach((tool, ti) => this.applyTool(tool, ti, si, section, raws, step.stepNumber));
		return this.schedule(step, si, start, raws);
	}

	private schedule(step: LessonStep, si: number, start: number, raws: Raw[]): StepTiming {
		const words = step.narration.split(/\s+/).filter(Boolean);
		const narrationDur = words.length / WORDS_PER_SECOND + 0.4;
		const rawTotal = raws.reduce((s, r) => s + r.dur, 0) || 1;
		const scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, (narrationDur - STEP_LEAD) / rawTotal));
		let t = start + STEP_LEAD;
		for (const r of raws) {
			r.item.start = t;
			r.item.dur = r.dur * scale;
			t += r.dur * scale;
		}
		this.applyErasures(raws);
		return { index: si, start, end: Math.max(start + narrationDur, t) + STEP_TAIL, narrationDur, words };
	}

	private applyErasures(raws: Raw[]): void {
		for (const r of raws) {
			const pending = (r.item as SceneItem & { eraseTargets?: string[] }).eraseTargets;
			if (pending) {
				this.items.filter((i) => pending.includes(i.id)).forEach((i) => (i.erasedAt = r.item.start));
			}
			const old = this.replacements.get(r.item.id);
			old?.forEach((oid) => this.markErased(oid, r.item.start));
		}
	}

	private newId(prefix: string): string {
		return `${prefix}-${this.counter++}`;
	}

	private base(step: number, ref: string | undefined, section: number, layer: Layer): ItemBase {
		return { id: this.newId('i'), ref, step, start: 0, dur: 0, erasedAt: null, section, layer };
	}

	private textItem(e: ColumnEntry, step: number, ref?: string): Raw {
		const item: TextItem = {
			...this.base(step, ref, this.sectionOf(step).index, 'col'), type: e.kind, x: e.x, y: e.y, w: e.w, h: e.h, fontSize: e.size, color: e.color,
			content: e.content, html: e.html, heading: e.heading,
		};
		this.items.push(item);
		const dur = e.kind === 'math' ? 0.9 + e.content.length * 0.012 : Math.max(0.6, e.content.length * 0.045);
		return { item, dur };
	}

	private register(ref: string | undefined, kind: string, bbox: Rect, anchors: Record<string, Pt>, frame: Frame | null, id: string): Entity {
		const item = this.items.find((i) => i.id === id)!;
		const entity: Entity = { ref: ref ?? id, kind, anchors, bbox, frame, itemIds: [id], section: item.section, layer: item.layer };
		if (ref) {
			const old = this.entities.get(ref);
			if (old) {
				this.replacements.set(id, old.itemIds);
			}
			this.entities.set(ref, entity);
		}
		return entity;
	}

	private markErased(oldId: string, at: number): void {
		const old = this.items.find((i) => i.id === oldId);
		if (old) {
			old.erasedAt = at;
		}
	}

	private applyTool(tool: WhiteboardTool, ti: number, si: number, section: Section, raws: Raw[], stepNo: number): void {
		switch (tool.tool) {
			case 'write': return this.write(tool, ti, si, section, raws);
			case 'sketch': return this.drawBuilt(tool.ref, tool.kind, this.sketch(section, tool.kind, tool.params ?? {}), section, si, raws, stepNo, ti);
			case 'diagram': return this.drawBuilt(tool.ref, `diagram:${tool.kind}`, buildDiagram(this.frameOf(section), tool.kind, tool.nodes, tool.links), section, si, raws, stepNo, ti);
			case 'image': return this.drawBuilt(tool.ref, 'image', imageBox(slotCenter(this.frameOf(section)), tool.prompt), section, si, raws, stepNo, ti);
			case 'connect': return this.connect(tool, section, si, raws, stepNo, ti);
			case 'emphasize':
			case 'highlight':
			case 'fill': return this.highlight(tool, si, raws);
			case 'erase': return this.erase(tool.target, si, raws);
			case 'clear_board': return this.clear(si, raws);
			default: return;
		}
	}

	private write(tool: Extract<WhiteboardTool, { tool: 'write' }>, ti: number, si: number, section: Section, raws: Raw[]): void {
		const entry = section.column.find((e) => e.step === si && e.tool === ti)!;
		const raw = this.textItem(entry, si, tool.ref);
		raws.push(raw);
		const rect = { x: entry.x, y: entry.y, w: entry.w, h: entry.h };
		this.register(tool.ref, entry.kind, rect, rectAnchors(rect), null, raw.item.id);
	}

	private ctx(frame: Frame): BuildCtx {
		return {
			frame,
			resolve: (spec) => this.resolve(spec),
			entity: (ref) => (ref ? this.entities.get(ref.split(/[.@]/)[0]) : undefined),
		};
	}

	private resolve(spec: string | undefined): Pt | undefined {
		const m = spec ? /^([\w-]+)(?:\.(\w+)|@([\d.]+))?$/.exec(spec.trim()) : null;
		const entity = m ? this.entities.get(m[1]) : undefined;
		if (!m || !entity) {
			return undefined;
		}
		if (m[2]) {
			return entity.anchors[m[2]];
		}
		const a = entity.anchors['from'] ?? entity.anchors['start'];
		const b = entity.anchors['to'] ?? entity.anchors['end'];
		if (m[3] && a && b) {
			const t = Number(m[3]);
			return { x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t };
		}
		return entity.anchors['center'] ?? { x: entity.bbox.x + entity.bbox.w / 2, y: entity.bbox.y + entity.bbox.h / 2 };
	}

	private sketch(section: Section, kind: string, params: SketchParams): Built | null {
		return buildSketch(this.ctx(this.frameOf(section)), kind, params);
	}

	private drawBuilt(ref: string, kind: string, built: Built | null, section: Section, si: number, raws: Raw[], stepNo: number, ti: number): void {
		if (!built) {
			this.fail(stepNo, ti, kind, `could not draw "${kind}"`);
			return;
		}
		const frame = this.frameOf(section);
		const item = this.shapeItem(built, frame, ref, si);
		const bbox = bboxOf(built.strokes);
		const entity = this.register(ref, kind, bbox, built.anchors, frame, item.id);
		if (kind === 'arrow') {
			frame.lastArrow = entity;
		}
		if (kind === 'axes') {
			frame.axes = entity;
		}
		raws.push({ item, dur: Math.max(0.6, Math.min(2.4, 0.4 + this.totalLength(built.strokes) / STROKE_SPEED)) });
	}

	private totalLength(strokes: Stroke[]): number {
		return strokes.reduce((s, st) => s + lengthOf(samplePoints(st)), 0);
	}

	private fail(step: number, index: number, tool: string, reason: string): void {
		this.replies.push({ step, index, tool, ok: false, message: `error: ${reason}, so NOTHING was drawn. Do not refer to it.` });
	}

	private shapeItem(built: Built, frame: Frame | null, ref: string | undefined, si: number): ShapeItem {
		const total = this.totalLength(built.strokes) || 1;
		const strokes: StrokeDraw[] = [];
		const guide: GuidePoint[] = [];
		let run = 0;
		const seedBase = hash(`${ref ?? 'x'}-${this.counter}`);
		built.strokes.forEach((s, i) => {
			const pts = samplePoints(s);
			const len = lengthOf(pts);
			strokes.push({ paths: roughPaths(s, seedBase + i), from: run / total, to: (run + len) / total });
			let acc = run;
			pts.forEach((p, k) => {
				acc += k ? Math.hypot(p[0] - pts[k - 1][0], p[1] - pts[k - 1][1]) : 0;
				guide.push({ x: p[0], y: p[1], at: acc / total });
			});
			run += len;
		});
		this.registerSegments(frame, built.strokes);
		const item: ShapeItem = { ...this.base(si, ref, frame?.id ?? this.sectionOf(si).index, 'fig'), type: 'shape', strokes, labels: [], guide };
		this.pending.push({ item, frame, reqs: built.labels });
		this.items.push(item);
		return item;
	}

	private bucket(frame: Frame | null): { segs: [XY, XY][]; rects: Rect[] } {
		const key = frame?.id ?? -1;
		if (!this.obstacles.has(key)) {
			this.obstacles.set(key, { segs: [], rects: [] });
		}
		return this.obstacles.get(key)!;
	}

	private registerSegments(frame: Frame | null, strokes: Stroke[]): void {
		const b = this.bucket(frame);
		for (const s of strokes) {
			const pts = samplePoints(s);
			pts.slice(1).forEach((p, i) => b.segs.push([pts[i], p]));
		}
	}

	private placeLabel(frame: Frame | null, req: LabelReq): LabelDraw {
		const b = this.bucket(frame);
		const base = req.size ?? LABEL_SIZE;
		let best: { c: Candidate; score: number; size: number } | null = null;
		for (const size of [base, base * 0.85]) {
			const found = this.bestCandidate(req, size, frame, b);
			if (!best || found.score < best.score) {
				best = { ...found, size };
			}
			if (found.score === 0) {
				break;
			}
		}
		b.rects.push(best!.c.rect);
		return { text: req.text, x: best!.c.x, y: best!.c.y, anchor: best!.c.anchor, size: best!.size, color: req.color ?? 'ink' };
	}

	private bestCandidate(req: LabelReq, size: number, frame: Frame | null, b: { segs: [XY, XY][]; rects: Rect[] }): { c: Candidate; score: number } {
		const w = req.text.length * size * 0.5;
		const h = size * 1.2;
		const candidates = req.fixed ? [centered(req.at, w, h, size)] : candidatesFor(req, w, h, size);
		let best = { c: candidates[0], score: Infinity };
		for (const c of candidates) {
			const score = collisions(c.rect, b) + outside(c.rect, frame) * 4;
			if (score < best.score) {
				best = { c, score };
			}
			if (score === 0) {
				break;
			}
		}
		return best;
	}

	private connect(tool: Extract<WhiteboardTool, { tool: 'connect' }>, section: Section, si: number, raws: Raw[], stepNo: number, ti: number): void {
		const a = this.entities.get(tool.from.split(/[.@]/)[0]);
		const b = this.entities.get(tool.to.split(/[.@]/)[0]);
		if (!a || !b) {
			return;
		}
		const ca = center(a.bbox);
		const cb = center(b.bbox);
		const dist = Math.hypot(ca.x - cb.x, ca.y - cb.y);
		if (dist > MAX_CONNECT_DISTANCE) {
			this.fail(stepNo, ti, 'connect', `the endpoints are ${Math.round(dist)}px apart, so an arrow would cross the board`);
			return;
		}
		const from = clip(a.bbox, cb);
		const to = clip(b.bbox, ca);
		const built: Built = {
			strokes: arrow(from, to, tool.color ?? 'blue'),
			labels: tool.label ? [{ text: tool.label, at: { x: (from.x + to.x) / 2, y: (from.y + to.y) / 2 }, prefer: 'above' }] : [],
			anchors: { from, to },
		};
		const frame = a.frame ?? b.frame ?? this.frameOf(section);
		const item = this.shapeItem(built, frame, undefined, si);
		raws.push({ item, dur: 0.7 });
	}

	private highlight(tool: Extract<WhiteboardTool, { tool: 'emphasize' | 'highlight' | 'fill' }>, si: number, raws: Raw[]): void {
		const target = this.entities.get(tool.target.split(/[.@]/)[0]);
		if (!target) {
			return;
		}
		const r = target.bbox;
		const item: HighlightItem = {
			...this.base(si, undefined, target.section, target.layer), type: 'highlight', x: r.x - 8, y: r.y - 4, w: r.w + 16, h: r.h + 8,
			color: tool.color ?? 'yellow', solid: tool.tool === 'fill',
		};
		this.items.push(item);
		raws.push({ item, dur: 0.6 });
	}

	private erase(target: string, si: number, raws: Raw[]): void {
		const entity = this.entities.get(target.split(/[.@]/)[0]);
		if (!entity) {
			return;
		}
		const marker = this.marker(si, entity.itemIds);
		raws.push({ item: marker, dur: 0.01 });
		this.entities.delete(target.split(/[.@]/)[0]);
	}

	private clear(si: number, raws: Raw[]): void {
		const ids = [...this.entities.values()].flatMap((e) => e.itemIds);
		raws.push({ item: this.marker(si, ids), dur: 0.01 });
		this.entities.clear();
	}

	private marker(si: number, targets: string[]): SceneItem {
		const item = { ...this.base(si, undefined, 0, 'col'), type: 'highlight', x: 0, y: 0, w: 0, h: 0, color: 'ink', solid: false, eraseTargets: targets } as HighlightItem;
		return item;
	}
}

function center(r: Rect): Pt {
	return { x: r.x + r.w / 2, y: r.y + r.h / 2 };
}

function clip(r: Rect, toward: Pt): Pt {
	const c = center(r);
	const dx = toward.x - c.x;
	const dy = toward.y - c.y;
	const k = Math.min(dx ? r.w / 2 / Math.abs(dx) : Infinity, dy ? r.h / 2 / Math.abs(dy) : Infinity, 1);
	return { x: c.x + dx * k, y: c.y + dy * k };
}

interface Candidate {
	x: number;
	y: number;
	anchor: 'start' | 'middle' | 'end';
	rect: Rect;
}

function centered(at: Pt, w: number, h: number, size: number): Candidate {
	return { x: at.x, y: at.y + size * 0.35, anchor: 'middle', rect: { x: at.x - w / 2, y: at.y - h / 2, w, h } };
}

function candidatesFor(req: LabelReq, w: number, h: number, size: number): Candidate[] {
	const order = ['above', 'below', 'right', 'left'];
	const dirs = req.prefer === 'center' ? ['center', ...order] : [req.prefer, ...order.filter((d) => d !== req.prefer)];
	const out: Candidate[] = [];
	for (const gap of [8, 24, 44]) {
		for (const dir of dirs) {
			out.push(candidate(dir, req.at, gap, w, h, size));
		}
	}
	return out;
}

function candidate(dir: string, at: Pt, gap: number, w: number, h: number, size: number): Candidate {
	switch (dir) {
		case 'above': return { x: at.x, y: at.y - gap, anchor: 'middle', rect: { x: at.x - w / 2, y: at.y - gap - h * 0.8, w, h } };
		case 'below': return { x: at.x, y: at.y + gap + h * 0.8, anchor: 'middle', rect: { x: at.x - w / 2, y: at.y + gap, w, h } };
		case 'right': return { x: at.x + gap, y: at.y + size * 0.35, anchor: 'start', rect: { x: at.x + gap, y: at.y - h / 2, w, h } };
		case 'left': return { x: at.x - gap, y: at.y + size * 0.35, anchor: 'end', rect: { x: at.x - gap - w, y: at.y - h / 2, w, h } };
		default: return centered(at, w, h, size);
	}
}

function collisions(r: Rect, b: { segs: [XY, XY][]; rects: Rect[] }): number {
	const overlapRects = b.rects.filter((o) => r.x < o.x + o.w && o.x < r.x + r.w && r.y < o.y + o.h && o.y < r.y + r.h).length;
	return overlapRects * 2 + b.segs.filter((s) => segmentHitsRect(s, r)).length;
}

function outside(r: Rect, frame: Frame | null): number {
	if (!frame) {
		return 0;
	}
	const inside = r.x >= frame.x - 20 && r.y >= frame.y - 10 && r.x + r.w <= frame.x + FRAME_W + 20 && r.y + r.h <= frame.y + FRAME_H + 10;
	return inside ? 0 : 1;
}

function segmentHitsRect([a, b]: [XY, XY], r: Rect): boolean {
	const inside = (p: XY): boolean => p[0] >= r.x && p[0] <= r.x + r.w && p[1] >= r.y && p[1] <= r.y + r.h;
	if (inside(a) || inside(b)) {
		return true;
	}
	const edges: [XY, XY][] = [
		[[r.x, r.y], [r.x + r.w, r.y]], [[r.x + r.w, r.y], [r.x + r.w, r.y + r.h]],
		[[r.x + r.w, r.y + r.h], [r.x, r.y + r.h]], [[r.x, r.y + r.h], [r.x, r.y]],
	];
	return edges.some((e) => segmentsCross(a, b, e[0], e[1]));
}

function segmentsCross(p1: XY, p2: XY, p3: XY, p4: XY): boolean {
	const d = (p2[0] - p1[0]) * (p4[1] - p3[1]) - (p2[1] - p1[1]) * (p4[0] - p3[0]);
	if (d === 0) {
		return false;
	}
	const t = ((p3[0] - p1[0]) * (p4[1] - p3[1]) - (p3[1] - p1[1]) * (p4[0] - p3[0])) / d;
	const u = ((p3[0] - p1[0]) * (p2[1] - p1[1]) - (p3[1] - p1[1]) * (p2[0] - p1[0])) / d;
	return t >= 0 && t <= 1 && u >= 0 && u <= 1;
}
