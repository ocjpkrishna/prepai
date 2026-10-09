import { LessonResponse } from '../models/lesson.model';

/** A short three-step lesson for the player while the backend is not reachable. */
export const MOCK_FULL_LESSON: LessonResponse = {
  lessonId: '11111111-1111-1111-1111-111111111111',
  title: 'Projectile Motion: Angled Launch',
  subject: 'PHYSICS',
  topic: 'Kinematics',
  difficulty: 'MEDIUM',
  totalSteps: 3,
  estimatedDurationSeconds: 60,
  steps: [
    {
      stepNumber: 1,
      title: 'Resolve velocity into components',
      narration: 'First, we break the initial velocity of twenty metres per second into horizontal and vertical components.',
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
        ],
      },
      equations: [{ latex: 'u_x = u \\cos 60° = 10 \\text{ m/s}', highlight: true, position: { x: 520, y: 150 } }],
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
        ],
      },
      equations: [{ latex: 'T = \\frac{2u_y}{g} = 2\\sqrt{3} \\text{ s}', highlight: true, position: { x: 520, y: 150 } }],
    },
    {
      stepNumber: 3,
      title: 'Find the range',
      narration: 'The range is the horizontal speed multiplied by the time of flight.',
      canvas: {
        actions: [
          {
            type: 'DRAW_DOUBLE_ARROW',
            config: { from: { x: 100, y: 320 }, to: { x: 450, y: 320 }, label: 'R = 20√3 m', color: '#E67E22' },
            animationDuration: 600,
          },
        ],
      },
      equations: [{ latex: 'R = u_x T = 20\\sqrt{3} \\text{ m}', highlight: true, position: { x: 520, y: 150 } }],
    },
  ],
  summary: {
    narration: 'So the projectile stays up for two root three seconds and lands about thirty five metres away.',
    keyResults: [
      { label: 'Time of flight', value: '2√3 s' },
      { label: 'Range', value: '20√3 m' },
    ],
  },
  masteryCheck: {
    question: 'What is the range if the launch angle is 30 degrees at the same speed?',
    options: [
      { id: 'a', text: '20√3 m', correct: true },
      { id: 'b', text: '10 m', correct: false },
    ],
    explanation: 'Range is the same for complementary angles.',
  },
};
