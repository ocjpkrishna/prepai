import { DiagramLink, DiagramNode, Pt } from '../../../core/models/whiteboard.model';
import { arrow, Built, Frame, LabelReq, segment, Stroke, XY } from './sketch-geometry';
import { Rect } from './scene.model';

const NODE_H = 50;
const NODE_MIN_W = 110;
const NODE_MAX_W = 190;
const CHAR_W = 10;
const PAD = 28;

interface Placed {
	node: DiagramNode;
	rect: Rect;
}

function nodeRect(node: DiagramNode, c: Pt): Rect {
	const w = Math.min(NODE_MAX_W, Math.max(NODE_MIN_W, node.label.length * CHAR_W + PAD));
	return { x: c.x - w / 2, y: c.y - NODE_H / 2, w, h: NODE_H };
}

function boxStroke(r: Rect): Stroke {
	return {
		kind: 'polygon', color: 'ink',
		pts: [[r.x, r.y], [r.x + r.w, r.y], [r.x + r.w, r.y + r.h], [r.x, r.y + r.h]],
	};
}

function nodeLabel(p: Placed): LabelReq {
	return { text: p.node.label, at: { x: p.rect.x + p.rect.w / 2, y: p.rect.y + p.rect.h / 2 }, prefer: 'center', fixed: true, size: 18 };
}

function edgePoint(r: Rect, toward: Pt): Pt {
	const c = { x: r.x + r.w / 2, y: r.y + r.h / 2 };
	const dx = toward.x - c.x;
	const dy = toward.y - c.y;
	const k = Math.min(dx ? r.w / 2 / Math.abs(dx) : Infinity, dy ? r.h / 2 / Math.abs(dy) : Infinity);
	return { x: c.x + dx * k, y: c.y + dy * k };
}

function links(placed: Placed[], given: DiagramLink[] | undefined, chain: boolean): { strokes: Stroke[]; labels: LabelReq[] } {
	const byId = new Map(placed.map((p) => [p.node.id, p]));
	const list = given?.length ? given : chain ? placed.slice(1).map((p, i) => ({ from: placed[i].node.id, to: p.node.id })) : [];
	const strokes: Stroke[] = [];
	const labels: LabelReq[] = [];
	for (const link of list) {
		const a = byId.get(link.from)?.rect;
		const b = byId.get(link.to)?.rect;
		if (!a || !b) {
			continue;
		}
		const ca = { x: a.x + a.w / 2, y: a.y + a.h / 2 };
		const cb = { x: b.x + b.w / 2, y: b.y + b.h / 2 };
		strokes.push(...arrow(edgePoint(a, cb), edgePoint(b, ca), 'blue'));
		if ((link as DiagramLink).label) {
			labels.push({ text: (link as DiagramLink).label!, at: { x: (ca.x + cb.x) / 2, y: (ca.y + cb.y) / 2 }, prefer: 'above', size: 16 });
		}
	}
	return { strokes, labels };
}

function finish(placed: Placed[], extra: Stroke[], given: DiagramLink[] | undefined, chain: boolean): Built {
	const l = links(placed, given, chain);
	const strokes = [...placed.map((p) => boxStroke(p.rect)), ...l.strokes, ...extra];
	const anchors: Record<string, Pt> = {};
	placed.forEach((p) => (anchors[p.node.id] = { x: p.rect.x + p.rect.w / 2, y: p.rect.y + p.rect.h / 2 }));
	return { strokes, labels: [...placed.map(nodeLabel), ...l.labels], anchors };
}

function flow(f: Frame, nodes: DiagramNode[], given?: DiagramLink[]): Built {
	const colX = [f.x + 130, f.x + f.w - 130];
	const placed = nodes.map((node, i) => {
		const row = Math.floor(i / 2);
		const col = row % 2 === 0 ? i % 2 : 1 - (i % 2);
		return { node, rect: nodeRect(node, { x: colX[col], y: f.y + 50 + row * 90 }) };
	});
	return finish(placed, [], given, true);
}

function tree(f: Frame, nodes: DiagramNode[], given?: DiagramLink[]): Built {
	const depth = (n: DiagramNode): number => (n.parent ? 1 + depth(nodes.find((x) => x.id === n.parent) ?? { id: '', label: '' }) : 0);
	const levels = new Map<number, DiagramNode[]>();
	nodes.forEach((n) => levels.set(depth(n), [...(levels.get(depth(n)) ?? []), n]));
	const placed: Placed[] = [];
	levels.forEach((row, d) =>
		row.forEach((node, i) =>
			placed.push({ node, rect: nodeRect(node, { x: f.x + (f.w * (i + 1)) / (row.length + 1), y: f.y + 50 + d * 100 }) })));
	const implied = nodes.filter((n) => n.parent).map((n) => ({ from: n.parent!, to: n.id }));
	return finish(placed, [], given?.length ? given : implied, false);
}

function columns(f: Frame, nodes: DiagramNode[]): Built {
	const groups = [...new Set(nodes.map((n) => n.group ?? ''))];
	const placed: Placed[] = [];
	const heads: LabelReq[] = [];
	groups.forEach((g, gi) => {
		const x = f.x + (f.w * (gi + 1)) / (groups.length + 1);
		heads.push({ text: g, at: { x, y: f.y + 24 }, prefer: 'center', fixed: true, size: 20, color: 'purple' });
		nodes.filter((n) => (n.group ?? '') === g).forEach((node, i) => placed.push({ node, rect: nodeRect(node, { x, y: f.y + 80 + i * 70 }) }));
	});
	const built = finish(placed, [], undefined, false);
	return { ...built, labels: [...built.labels, ...heads] };
}

function timeline(f: Frame, nodes: DiagramNode[]): Built {
	const y = f.y + f.h / 2 - 20;
	const x0 = f.x + 30;
	const x1 = f.x + f.w - 30;
	const strokes: Stroke[] = [...arrow({ x: x0, y }, { x: x1, y }, 'ink')];
	const labels: LabelReq[] = [];
	const anchors: Record<string, Pt> = {};
	nodes.forEach((n, i) => {
		const x = x0 + 30 + ((x1 - x0 - 60) * i) / Math.max(1, nodes.length - 1);
		const up = i % 2 === 0;
		strokes.push(segment({ x, y: y - 8 }, { x, y: y + 8 }, 'ink'));
		labels.push({ text: n.label, at: { x, y: y + (up ? -26 : 34) }, prefer: 'center', fixed: true, size: 18 });
		anchors[n.id] = { x, y };
	});
	return { strokes, labels, anchors };
}

function network(f: Frame, nodes: DiagramNode[], given?: DiagramLink[]): Built {
	const c = { x: f.x + f.w / 2, y: f.y + f.h / 2 - 20 };
	const placed = nodes.map((node, i) => {
		const a = -Math.PI / 2 + (i * 2 * Math.PI) / nodes.length;
		return { node, rect: nodeRect(node, { x: c.x + 170 * Math.cos(a), y: c.y + 130 * Math.sin(a) }) };
	});
	return finish(placed, [], given, false);
}

export function buildDiagram(f: Frame, kind: string, nodes: DiagramNode[], given?: DiagramLink[]): Built | null {
	switch (kind) {
		case 'flow': return flow(f, nodes, given);
		case 'tree': return tree(f, nodes, given);
		case 'columns': return columns(f, nodes);
		case 'timeline': return timeline(f, nodes);
		case 'network': return network(f, nodes, given);
		default: return null;
	}
}

export function imageBox(c: Pt, prompt: string): Built {
	const r = { x: c.x - 110, y: c.y - 75, w: 220, h: 150 };
	const stroke: Stroke = {
		kind: 'polygon', color: 'ink', dashed: true,
		pts: [[r.x, r.y], [r.x + r.w, r.y], [r.x + r.w, r.y + r.h], [r.x, r.y + r.h]] as XY[],
	};
	const text = prompt.length > 28 ? `${prompt.slice(0, 26)}...` : prompt;
	return {
		strokes: [stroke],
		labels: [{ text: `picture: ${text}`, at: c, prefer: 'center', fixed: true, size: 16 }],
		anchors: { center: c },
	};
}
