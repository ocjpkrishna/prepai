import { describe, expect, it } from 'vitest';
import { MOCK_LESSONS } from '../../../core/mock/mock-lessons';
import { Lesson } from '../../../core/models/whiteboard.model';
import { buildScene } from './board-layout';
import { BOARD_WIDTH, LabelDraw, ShapeItem } from './scene.model';
import { estimateMeasurer } from './text-measure';
import { validateLesson } from './tool-validator';

const measure = estimateMeasurer();

function labelRect(l: LabelDraw): { x: number; y: number; w: number; h: number } {
	const w = l.text.length * l.size * 0.5;
	const x = l.anchor === 'middle' ? l.x - w / 2 : l.anchor === 'end' ? l.x - w : l.x;
	return { x, y: l.y - l.size, w, h: l.size * 1.2 };
}

function overlap(a: ReturnType<typeof labelRect>, b: ReturnType<typeof labelRect>): boolean {
	return a.x < b.x + b.w && b.x < a.x + a.w && a.y < b.y + b.h && b.y < a.y + a.h;
}

describe('whiteboard scene', () => {
	for (const lesson of MOCK_LESSONS) {
		describe(lesson.title, () => {
			const scene = buildScene(lesson, measure);

			it('has no error replies', () => {
				const errors = scene.replies.filter((r) => !r.ok);
				expect(errors.map((e) => e.message)).toEqual([]);
			});

			it('keeps every item on the board', () => {
				for (const item of scene.items) {
					if (item.type === 'shape') {
						item.guide.forEach((g) => {
							expect(g.x).toBeGreaterThan(-5);
							expect(g.x).toBeLessThan(BOARD_WIDTH + 5);
							expect(g.y).toBeGreaterThan(-5);
							expect(g.y).toBeLessThan(scene.height);
						});
					} else if (item.type !== 'highlight') {
						expect(item.x + item.w).toBeLessThanOrEqual(BOARD_WIDTH + 1);
					}
				}
			});

			it('places no two labels on top of each other', () => {
				const labels = scene.items.filter((i): i is ShapeItem => i.type === 'shape').flatMap((s) => s.labels);
				for (let i = 0; i < labels.length; i++) {
					for (let j = i + 1; j < labels.length; j++) {
						expect(overlap(labelRect(labels[i]), labelRect(labels[j])), `${labels[i].text} vs ${labels[j].text}`).toBe(false);
					}
				}
			});

			it('plays steps in order', () => {
				scene.steps.forEach((s, i) => {
					expect(s.end).toBeGreaterThan(s.start);
					if (i > 0) {
						expect(s.start).toBe(scene.steps[i - 1].end);
					}
				});
			});
		});
	}
});

describe('tool validator', () => {
	const base: Lesson = { ...MOCK_LESSONS[2], steps: [] };
	const withTools = (tools: Lesson['steps'][0]['tools'], narration = 'x'): Lesson => ({
		...base, steps: [{ stepNumber: 1, title: 't', narration, tools }],
	});

	it('rejects a sentence sent as math', () => {
		const r = validateLesson(withTools([{ tool: 'write', ref: 'a', kind: 'math', content: 'this line is a plain sentence about motion' }]));
		expect(r.replies[0].message).toContain('that line was a sentence');
		expect(r.steps[0].tools).toHaveLength(0);
	});

	it('rejects unknown tools and kinds with the fix in the message', () => {
		const r = validateLesson(withTools([
			{ tool: 'sketch', ref: 'a', kind: 'dragon' },
			{ tool: 'sparkle' } as never,
		]));
		expect(r.replies[0].message).toContain('NOTHING was drawn');
		expect(r.replies[1].message).toContain('unknown tool');
	});

	it('rejects references to refs that do not exist', () => {
		const r = validateLesson(withTools([{ tool: 'connect', from: 'a', to: 'b' }]));
		expect(r.replies[0].ok).toBe(false);
	});

	it('requires a plan before begin_concept', () => {
		const lesson = { ...withTools([{ tool: 'begin_concept', id: 'zz' }]), plan: [] };
		expect(validateLesson(lesson).replies[0].message).toContain('no lesson plan');
	});

	it('flags narration about a wave with no wave on the board', () => {
		const r = validateLesson(withTools([{ tool: 'write', ref: 'a', kind: 'text', content: 'hi' }], 'The sine wave has a long wavelength'));
		expect(r.replies.some((x) => x.tool === 'narration')).toBe(true);
	});

	it('rejects move for now', () => {
		const r = validateLesson(withTools([{ tool: 'move', target: 'a' }]));
		expect(r.replies[0].message).toContain('not supported');
	});
});
