import {
	COLOR_NAMES, DIAGRAM_KINDS, Lesson, LessonStep, SKETCH_KINDS, SketchParams, ToolReply, WhiteboardTool,
} from '../../../core/models/whiteboard.model';

const TOOL_NAMES = [
	'write', 'sketch', 'diagram', 'image', 'connect', 'emphasize', 'highlight', 'fill',
	'erase', 'move', 'begin_concept', 'clear_board', 'new_page',
];
const MAX_TOOLS_PER_STEP = 40;
const MAX_DIAGRAM_NODES = 8;
const PROSE_WORDS = 6;
const WAVE_WORDS = /\b(sine|cosine|wave|waveform|waves|frequency|amplitude|wavelength)\b/i;
const WAVE_KINDS = ['sine_wave', 'square_wave', 'sawtooth', 'curve'];
const REF_PARAMS: (keyof SketchParams)[] = ['from', 'to', 'at', 'on', 'spans', 'dropTo'];

export interface ValidationResult {
	replies: ToolReply[];
	steps: LessonStep[];
}

export function validateLesson(lesson: Lesson): ValidationResult {
	const refs = new Set<string>();
	const kindsSeen = new Set<string>();
	const replies: ToolReply[] = [];
	const steps = lesson.steps.map((step) => {
		const kept = validateStep(lesson, step, { refs, kindsSeen, replies });
		checkNarration(step, kindsSeen, replies);
		return { ...step, tools: kept };
	});
	return { replies, steps };
}

interface Ctx {
	refs: Set<string>;
	kindsSeen: Set<string>;
	replies: ToolReply[];
}

function validateStep(lesson: Lesson, step: LessonStep, ctx: Ctx): WhiteboardTool[] {
	const kept: WhiteboardTool[] = [];
	step.tools.slice(0, MAX_TOOLS_PER_STEP).forEach((tool, index) => {
		const message = check(lesson, tool, ctx);
		const ok = message === null;
		ctx.replies.push({ step: step.stepNumber, index, tool: tool?.tool ?? '?', ok, message: ok ? 'ok' : message });
		if (ok) {
			kept.push(tool);
			register(tool, ctx);
		}
	});
	return kept;
}

function register(tool: WhiteboardTool, ctx: Ctx): void {
	if ('ref' in tool && tool.ref) {
		ctx.refs.add(tool.ref);
	}
	if (tool.tool === 'sketch' || tool.tool === 'diagram') {
		ctx.kindsSeen.add(tool.kind);
	}
	if (tool.tool === 'erase') {
		ctx.refs.delete(tool.target);
	}
	if (tool.tool === 'clear_board') {
		ctx.refs.clear();
	}
}

function fail(reason: string, fix: string): string {
	return `error: ${reason}, so NOTHING was drawn. Do not refer to it. ${fix}`;
}

function check(lesson: Lesson, tool: WhiteboardTool, ctx: Ctx): string | null {
	if (!tool || !TOOL_NAMES.includes(tool.tool)) {
		return fail(`unknown tool "${tool?.tool}"`, `Use one of: ${TOOL_NAMES.join(', ')}.`);
	}
	switch (tool.tool) {
		case 'write': return checkWrite(tool);
		case 'sketch': return checkSketch(tool, ctx);
		case 'diagram': return checkDiagram(tool);
		case 'image': return tool.ref && tool.prompt ? null : fail('image needs a ref and a prompt', 'Give both.');
		case 'connect': return checkConnect(tool, ctx);
		case 'emphasize':
		case 'highlight':
		case 'fill': return checkTarget(tool.target, ctx) ?? checkColor(tool.color);
		case 'erase': return checkTarget(tool.target, ctx);
		case 'move': return fail('move is not supported yet', 'Erase the object and draw it again.');
		case 'begin_concept': return checkConcept(lesson, tool.id);
		default: return null;
	}
}

function checkWrite(tool: Extract<WhiteboardTool, { tool: 'write' }>): string | null {
	if (!tool.ref || !tool.content?.trim()) {
		return fail('write needs a ref and content', 'Give both.');
	}
	if (tool.kind !== 'text' && tool.kind !== 'math') {
		return fail(`write kind "${tool.kind}" is unknown`, 'Use kind "text" or "math".');
	}
	if (tool.kind === 'math' && looksLikeProse(tool.content)) {
		return fail('that line was a sentence', 'Send prose as kind "text"; kind "math" is for the formula itself.');
	}
	return checkColor(tool.color);
}

function looksLikeProse(content: string): boolean {
	const words = content.match(/[A-Za-z]{3,}/g) ?? [];
	const hasMath = /[=\\^_{}+\-*/<>]/.test(content);
	return words.length >= PROSE_WORDS && !hasMath;
}

function checkColor(color?: string): string | null {
	if (color && !(COLOR_NAMES as readonly string[]).includes(color)) {
		return fail(`color "${color}" is unknown`, `Use one of: ${COLOR_NAMES.join(', ')}.`);
	}
	return null;
}

function refName(spec: string): string {
	return spec.split(/[.@]/)[0].trim();
}

function missingRef(spec: string, ctx: Ctx): string | null {
	const names = spec.split(':').map(refName);
	return names.find((name) => !ctx.refs.has(name)) ?? null;
}

function checkSketch(tool: Extract<WhiteboardTool, { tool: 'sketch' }>, ctx: Ctx): string | null {
	if (!tool.ref) {
		return fail('sketch needs a ref', 'Give it a short ref.');
	}
	if (!(SKETCH_KINDS as readonly string[]).includes(tool.kind)) {
		return fail(`sketch kind "${tool.kind}" is unknown`, `Use one of: ${SKETCH_KINDS.join(', ')}.`);
	}
	const params = tool.params ?? {};
	for (const key of REF_PARAMS) {
		const value = params[key];
		const missing = typeof value === 'string' ? missingRef(value, ctx) : null;
		if (missing) {
			return fail(`"${missing}" does not exist on the board`, 'Draw it first, or use a ref that exists.');
		}
	}
	if (tool.kind === 'bracket' && !params.spans) {
		return fail('a bracket needs params.spans', 'Say which ref or anchors it spans.');
	}
	if (params.along && !ctx.kindsSeen.has('arrow')) {
		return fail('"along" needs an arrow to project', 'Draw the arrow first.');
	}
	return checkColor(params.color);
}

function checkDiagram(tool: Extract<WhiteboardTool, { tool: 'diagram' }>): string | null {
	if (!(DIAGRAM_KINDS as readonly string[]).includes(tool.kind)) {
		return fail(`diagram kind "${tool.kind}" is unknown`, `Use one of: ${DIAGRAM_KINDS.join(', ')}.`);
	}
	const nodes = tool.nodes ?? [];
	if (nodes.length < 2 || nodes.length > MAX_DIAGRAM_NODES) {
		return fail(`a diagram needs 2 to ${MAX_DIAGRAM_NODES} nodes`, 'Split it into smaller diagrams.');
	}
	const ids = new Set(nodes.map((node) => node.id));
	const bad = (tool.links ?? []).find((link) => !ids.has(link.from) || !ids.has(link.to));
	return bad ? fail(`link ${bad.from} to ${bad.to} names a missing node`, 'Use node ids that exist.') : null;
}

function checkConnect(tool: Extract<WhiteboardTool, { tool: 'connect' }>, ctx: Ctx): string | null {
	const missing = [tool.from, tool.to].find((ref) => !ref || !ctx.refs.has(refName(ref)));
	if (missing !== undefined) {
		return fail(`"${missing}" does not exist on the board`, 'Connect two refs that exist.');
	}
	return refName(tool.from) === refName(tool.to) ? fail('connect needs two different refs', 'Pick another end.') : null;
}

function checkTarget(target: string, ctx: Ctx): string | null {
	return ctx.refs.has(refName(target ?? '')) ? null : fail(`"${target}" does not exist on the board`, 'Use a ref that exists.');
}

function checkConcept(lesson: Lesson, id: string): string | null {
	if (!lesson.plan?.length) {
		return fail('there is no lesson plan', 'Give the lesson a plan before begin_concept.');
	}
	return lesson.plan.some((entry) => entry.id === id)
		? null
		: fail(`unknown concept id "${id}"`, `Use one of: ${lesson.plan.map((entry) => entry.id).join(', ')}.`);
}

function checkNarration(step: LessonStep, kindsSeen: Set<string>, replies: ToolReply[]): void {
	const wave = WAVE_WORDS.test(step.narration);
	if (wave && !WAVE_KINDS.some((kind) => kindsSeen.has(kind))) {
		replies.push({
			step: step.stepNumber, index: -1, tool: 'narration', ok: false,
			message: 'error: the narration describes a wave but none is drawn. Draw a sine_wave, square_wave or sawtooth, or reword it.',
		});
	}
}
