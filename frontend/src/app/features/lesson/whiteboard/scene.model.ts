import { Pt, ToolReply } from '../../../core/models/whiteboard.model';

export const BOARD_WIDTH = 1000;

export interface PathDraw {
	d: string;
	color: string;
	width: number;
	dash?: string;
	fill?: string;
}

export interface StrokeDraw {
	paths: PathDraw[];
	from: number;
	to: number;
}

export interface LabelDraw {
	text: string;
	x: number;
	y: number;
	anchor: 'start' | 'middle' | 'end';
	size: number;
	color: string;
}

export interface GuidePoint {
	x: number;
	y: number;
	at: number;
}

export type Layer = 'fig' | 'col';

export interface ItemBase {
	id: string;
	section: number;
	layer: Layer;
	ref?: string;
	step: number;
	start: number;
	dur: number;
	erasedAt: number | null;
}

export interface ShapeItem extends ItemBase {
	type: 'shape';
	strokes: StrokeDraw[];
	labels: LabelDraw[];
	guide: GuidePoint[];
}

export interface TextItem extends ItemBase {
	type: 'text' | 'math';
	x: number;
	y: number;
	w: number;
	h: number;
	fontSize: number;
	color: string;
	content: string;
	html?: string;
	heading?: boolean;
}

export interface HighlightItem extends ItemBase {
	type: 'highlight';
	x: number;
	y: number;
	w: number;
	h: number;
	color: string;
	solid: boolean;
}

export type SceneItem = ShapeItem | TextItem | HighlightItem;

export interface StepTiming {
	index: number;
	start: number;
	end: number;
	narrationDur: number;
	words: string[];
}

export interface SectionInfo {
	index: number;
	top: number;
	height: number;
	frame: Rect | null;
	colX: number;
	colW: number;
}

export interface Scene {
	width: number;
	height: number;
	sections: SectionInfo[];
	items: SceneItem[];
	steps: StepTiming[];
	duration: number;
	replies: ToolReply[];
}

export interface Rect {
	x: number;
	y: number;
	w: number;
	h: number;
}

export function center(rect: Rect): Pt {
	return { x: rect.x + rect.w / 2, y: rect.y + rect.h / 2 };
}
