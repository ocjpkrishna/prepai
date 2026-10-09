export interface Pt {
	x: number;
	y: number;
}

export const COLOR_NAMES = ['ink', 'blue', 'red', 'green', 'orange', 'purple', 'yellow', 'pink'] as const;
export type ColorName = (typeof COLOR_NAMES)[number];

export const SKETCH_KINDS = [
	'axes', 'curve', 'line', 'arrow', 'box', 'circle', 'bracket', 'table', 'number_line',
	'triangle', 'right_triangle', 'square', 'pentagon', 'hexagon',
	'sine_wave', 'square_wave', 'sawtooth', 'parabola', 'freeform',
] as const;
export type SketchKind = (typeof SKETCH_KINDS)[number];

export const DIAGRAM_KINDS = ['flow', 'tree', 'network', 'columns', 'timeline'] as const;
export type DiagramKind = (typeof DIAGRAM_KINDS)[number];

export interface SketchParams {
	label?: string;
	color?: string;
	dashed?: boolean;
	from?: string;
	to?: string;
	at?: string;
	on?: string;
	x?: number;
	spans?: string;
	side?: 'above' | 'below' | 'left' | 'right';
	along?: 'x' | 'y';
	dropTo?: string;
	ground?: boolean;
	launch?: 'horizontal' | 'angled';
	angle?: number;
	length?: number;
	height?: number;
	width?: number;
	size?: number;
	xLabel?: string;
	yLabel?: string;
	sides?: string[];
	min?: number;
	max?: number;
	marks?: { at: number; label?: string }[];
	rows?: string[][];
	points?: [number, number][];
}

export interface DiagramNode {
	id: string;
	label: string;
	group?: string;
	parent?: string;
}

export interface DiagramLink {
	from: string;
	to: string;
	label?: string;
}

export type WhiteboardTool =
	| { tool: 'write'; ref: string; kind: 'text' | 'math'; content: string; size?: number; color?: string }
	| { tool: 'sketch'; ref: string; kind: string; params?: SketchParams }
	| { tool: 'diagram'; ref: string; kind: string; nodes: DiagramNode[]; links?: DiagramLink[] }
	| { tool: 'image'; ref: string; prompt: string }
	| { tool: 'connect'; from: string; to: string; label?: string; color?: string }
	| { tool: 'emphasize' | 'highlight' | 'fill'; target: string; part?: string; color?: string }
	| { tool: 'erase'; target: string }
	| { tool: 'move'; target: string; near?: string }
	| { tool: 'begin_concept'; id: string }
	| { tool: 'clear_board' }
	| { tool: 'new_page' };

export interface LessonStep {
	stepNumber: number;
	title: string;
	narration: string;
	tools: WhiteboardTool[];
}

export interface LessonPlanEntry {
	id: string;
	label: string;
}

export interface MasteryCheck {
	question: string;
	options: { id: string; text: string; correct: boolean }[];
	explanation: string;
}

export interface Lesson {
	lessonId: string;
	title: string;
	subject: string;
	topic: string;
	difficulty: string;
	plan: LessonPlanEntry[];
	steps: LessonStep[];
	summary: { narration: string; keyResults: { label: string; value: string }[] };
	masteryCheck?: MasteryCheck;
}

export interface ToolReply {
	step: number;
	index: number;
	tool: string;
	ok: boolean;
	message: string;
}
