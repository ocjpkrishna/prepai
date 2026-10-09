import { LessonStep } from '../../../../core/models/lesson.model';


/** The four steps of the spec 3.2 sample lesson (canvas and equations). */
export const SAMPLE_STEPS: LessonStep[] = [
  {
    stepNumber: 1,
    title: 'Resolve velocity into components',
    narration: 'First, we need to break the initial velocity into horizontal and vertical components.',
    canvas: {
      actions: [
        {
          type: 'DRAW_AXIS',
          config: { origin: { x: 100, y: 300 }, xLength: 400, yLength: 250, xLabel: 'Horizontal', yLabel: 'Vertical' },
          animationDuration: 1000,
        },
        {
          type: 'DRAW_ARROW',
          config: { from: { x: 100, y: 300 }, to: { x: 350, y: 100 }, label: 'u = 20 m/s', color: '#4A90D9', angle: 60 },
          animationDuration: 800,
        },
        {
          type: 'DRAW_DASHED_LINE',
          config: { from: { x: 100, y: 300 }, to: { x: 350, y: 300 }, label: 'uₓ = 10 m/s', color: '#E74C3C' },
          animationDuration: 600,
        },
        {
          type: 'DRAW_DASHED_LINE',
          config: { from: { x: 350, y: 300 }, to: { x: 350, y: 100 }, label: 'uᵧ = 10√3 m/s', color: '#2ECC71' },
          animationDuration: 600,
        },
        {
          type: 'DRAW_ARC',
          config: { center: { x: 100, y: 300 }, radius: 50, startAngle: 0, endAngle: 60, label: '60°' },
          animationDuration: 400,
        },
      ],
    },
    equations: [
      { latex: 'u_x = u \\cos 60° = 10 \\text{ m/s}', highlight: true, position: { x: 520, y: 150 } },
      { latex: 'u_y = 10\\sqrt{3} \\text{ m/s}', highlight: true, position: { x: 520, y: 200 } },
    ],
  },
  {
    stepNumber: 2,
    title: 'Calculate time of flight',
    narration: 'The time of flight is twice the vertical component divided by g.',
    canvas: {
      actions: [
        {
          type: 'DRAW_PARABOLA',
          config: { start: { x: 100, y: 300 }, peak: { x: 275, y: 100 }, end: { x: 450, y: 300 }, color: '#4A90D9', dashed: false },
          animationDuration: 1200,
        },
        {
          type: 'DRAW_DOUBLE_ARROW',
          config: { from: { x: 100, y: 320 }, to: { x: 450, y: 320 }, label: 'T = 2√3 s', color: '#E67E22' },
          animationDuration: 600,
        },
      ],
    },
    equations: [{ latex: 'T = \\frac{2u_y}{g} = 2\\sqrt{3} \\approx 3.46 \\text{ s}', highlight: true, position: { x: 520, y: 150 } }],
  },
  {
    stepNumber: 3,
    title: 'Calculate maximum height',
    narration: 'At the highest point the vertical velocity is zero.',
    canvas: {
      actions: [
        {
          type: 'DRAW_DASHED_LINE',
          config: { from: { x: 275, y: 300 }, to: { x: 275, y: 100 }, label: 'H = 15 m', color: '#9B59B6' },
          animationDuration: 600,
        },
        {
          type: 'DRAW_POINT',
          config: { position: { x: 275, y: 100 }, label: 'vᵧ = 0', color: '#E74C3C', radius: 5 },
          animationDuration: 300,
        },
      ],
    },
    equations: [{ latex: 'H = \\frac{u_y^2}{2g} = 15 \\text{ m}', highlight: true, position: { x: 520, y: 150 } }],
  },
  {
    stepNumber: 4,
    title: 'Calculate horizontal range',
    narration: 'The horizontal range is the horizontal velocity multiplied by the total time of flight.',
    canvas: {
      actions: [
        {
          type: 'DRAW_DOUBLE_ARROW',
          config: { from: { x: 100, y: 340 }, to: { x: 450, y: 340 }, label: 'R = 20√3 ≈ 34.64 m', color: '#27AE60' },
          animationDuration: 600,
        },
      ],
    },
    equations: [
      { latex: 'R = u_x \\times T = 20\\sqrt{3} \\approx 34.64 \\text{ m}', highlight: true, position: { x: 520, y: 150 } },
      { latex: '\\text{Verify: } R = \\frac{u^2 \\sin 2\\theta}{g} \\checkmark', highlight: false, position: { x: 520, y: 220 } },
    ],
  },
];

/** One valid action of every type in spec 3.3. Nothing in it should warn. */
export const ALL_TYPES_STEP: LessonStep = {
  stepNumber: 1,
  title: 'Every action type',
  narration: 'One action of each type.',
  canvas: {
    actions: [
      { type: 'DRAW_AXIS', config: { id: 'axis', origin: { x: 60, y: 460 }, xLength: 200, yLength: 150, xLabel: 'x', yLabel: 'y' } },
      { type: 'DRAW_ARROW', config: { from: { x: 60, y: 460 }, to: { x: 200, y: 380 }, label: 'v', color: '#4A90D9' } },
      { type: 'DRAW_DASHED_LINE', config: { from: { x: 60, y: 400 }, to: { x: 200, y: 400 }, label: 'd', color: '#E74C3C' } },
      { type: 'DRAW_LINE', config: { from: { x: 60, y: 360 }, to: { x: 200, y: 360 }, strokeWidth: 3 } },
      { type: 'DRAW_ARC', config: { center: { x: 60, y: 460 }, radius: 40, startAngle: 0, endAngle: 45, label: '45°' } },
      { type: 'DRAW_PARABOLA', config: { start: { x: 220, y: 460 }, peak: { x: 300, y: 300 }, end: { x: 380, y: 460 }, color: '#27AE60' } },
      { type: 'DRAW_CIRCLE', config: { center: { x: 300, y: 200 }, radius: 30, color: '#9B59B6', fill: true } },
      { type: 'DRAW_POINT', config: { position: { x: 300, y: 200 }, label: 'P', color: '#E74C3C', radius: 4 } },
      { type: 'DRAW_DOUBLE_ARROW', config: { from: { x: 220, y: 480 }, to: { x: 380, y: 480 }, label: 'R', color: '#E67E22' } },
      { type: 'WRITE_TEXT', config: { position: { x: 420, y: 60 }, text: 'Projectile', fontSize: 20, color: '#F3F4F6' } },
      { type: 'WRITE_LATEX', config: { position: { x: 420, y: 120 }, latex: 'R = \\frac{u^2\\sin 2\\theta}{g}', fontSize: 18 } },
      { type: 'DRAW_VECTOR', config: { origin: { x: 480, y: 300 }, magnitude: 5, angle: 30, label: 'F', color: '#2ECC71' } },
      {
        type: 'DRAW_FREE_BODY',
        config: {
          center: { x: 140, y: 240 },
          forces: [
            { angle: 90, magnitude: 3, label: 'N', color: '#4A90D9' },
            { angle: 270, magnitude: 3, label: 'mg', color: '#E74C3C' },
          ],
        },
      },
      {
        type: 'DRAW_CIRCUIT',
        config: {
          components: [
            { type: 'BATTERY', from: { x: 40, y: 80 }, to: { x: 40, y: 200 }, label: '12 V' },
            { type: 'RESISTOR', from: { x: 40, y: 200 }, to: { x: 180, y: 200 }, label: 'R' },
            { type: 'CAPACITOR', from: { x: 180, y: 200 }, to: { x: 180, y: 80 }, label: 'C' },
          ],
        },
      },
      { type: 'DRAW_GRAPH', config: { fn: 'x^2 - 4', xRange: [-4, 4], yRange: [-5, 12], color: '#4A90D9' } },
      { type: 'HIGHLIGHT_REGION', config: { points: [{ x: 420, y: 300 }, { x: 480, y: 300 }, { x: 450, y: 360 }], color: '#F1C40F', opacity: 0.3 } },
      { type: 'FADE_OUT', config: { elementIds: ['axis'] }, animationDuration: 200 },
      { type: 'CLEAR_CANVAS', config: { keepElements: [] } },
    ],
  },
  equations: [],
};

/** Bad data in every way the spec of agent 5 (task 9) names. The good action at the end must still draw. */
export const MALFORMED_STEP = {
  stepNumber: 1,
  title: 'Malformed on purpose',
  narration: 'Every action below is wrong in some way.',
  canvas: {
    actions: [
      { type: 'DRAW_HOVERCRAFT', config: { from: { x: 0, y: 0 } } },
      { type: 'DRAW_LINE', config: { from: { x: NaN, y: 10 }, to: { x: 100, y: 100 } } },
      { type: 'WRITE_TEXT', config: { position: { x: 10, y: 10 } } },
      { type: 'WRITE_LATEX', config: { position: { x: 10, y: 200 }, latex: '\\frac{1}{' } },
      { type: 'DRAW_GRAPH', config: { fn: 'x +* 2', xRange: [-1, 1], yRange: [-1, 1] } },
      { type: 'DRAW_ARROW', config: { from: { x: 900, y: -50 }, to: { x: 'far', y: 10 }, color: 'javascript:alert(1)' } },
      { type: 'DRAW_CIRCLE', config: { center: { x: 300, y: 300 }, radius: -20 } },
      'not an action',
      { config: { from: { x: 0, y: 0 }, to: { x: 10, y: 10 } } },
      { type: 'HIGHLIGHT_REGION', config: { points: [{ x: 1, y: 1 }, { x: 'x', y: 2 }] } },
      { type: 'DRAW_ARROW', config: { from: { x: 100, y: 400 }, to: { x: 300, y: 400 }, label: 'still drawn' }, animationDuration: -5 },
    ],
  },
  equations: [{ latex: '\\undefined{x}{', highlight: false, position: { x: 520, y: 150 } }],
} as unknown as LessonStep;
