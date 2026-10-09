import { TestBed } from '@angular/core/testing';
import { AnimationEngineService } from './animation-engine.service';
import { CanvasRendererService } from './canvas-renderer.service';
import { EquationRendererService } from './equation-renderer.service';
import { ALL_TYPES_STEP, MALFORMED_STEP, SAMPLE_STEPS } from './testing/whiteboard.fixtures';
import { RenderWarning } from './whiteboard.types';

vi.mock('konva', () => import('./testing/konva-fake'));

/** Each frame arrives after the whole animation, so every action finishes in one frame. */
const FRAME_TIME_MS = 1e6;

describe('CanvasRendererService', () => {
  let renderer: CanvasRendererService;
  let warnings: RenderWarning[];

  beforeEach(() => {
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) =>
      setTimeout(() => callback(performance.now() + FRAME_TIME_MS), 0));
    vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    TestBed.configureTestingModule({
      providers: [AnimationEngineService, EquationRendererService, CanvasRendererService],
    });
    renderer = TestBed.inject(CanvasRendererService);
    renderer.attach(document.createElement('div'), document.createElement('div'));
    warnings = [];
    renderer.warnings.subscribe((warning) => warnings.push(warning));
  });

  afterEach(() => {
    renderer.destroy();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('plays every sample step to its end without a warning', async () => {
    for (const step of SAMPLE_STEPS) {
      expect(await renderer.playStep(step, 'dark')).toBe(true);
    }
    expect(warnings).toEqual([]);
  });

  it('draws every action type in spec 3.3 without a warning', async () => {
    expect(await renderer.playStep(ALL_TYPES_STEP, 'light')).toBe(true);
    expect(warnings).toEqual([]);
  });

  it('plays a malformed step to the end, skips each bad action and reports it', async () => {
    expect(await renderer.playStep(MALFORMED_STEP, 'dark')).toBe(true);
    expect(warnings).toEqual(expect.arrayContaining([
      { actionIndex: 0, actionType: 'DRAW_HOVERCRAFT', message: 'unknown action type, skipped' },
      { actionIndex: 1, actionType: 'DRAW_LINE', message: 'could not be drawn, skipped: from is missing or invalid' },
      { actionIndex: 2, actionType: 'WRITE_TEXT', message: 'could not be drawn, skipped: text is missing or invalid' },
      { actionIndex: 3, actionType: 'WRITE_LATEX', message: 'latex does not parse, raw source shown' },
      { actionIndex: 4, actionType: 'DRAW_GRAPH', message: 'could not be drawn, skipped: fn is not a valid expression' },
      { actionIndex: 6, actionType: 'DRAW_CIRCLE', message: 'radius is outside its range, clamped' },
      { actionIndex: 7, actionType: 'UNKNOWN', message: 'action is not an object, skipped' },
      { actionIndex: 9, actionType: 'HIGHLIGHT_REGION', message: 'could not be drawn, skipped: points needs at least 3 valid entries' },
    ]));
  });

  it('keeps lessons going after a malformed step', async () => {
    await renderer.playStep(MALFORMED_STEP, 'dark');
    expect(await renderer.playStep(SAMPLE_STEPS[0], 'dark')).toBe(true);
  });

  it('stops an unfinished step when a newer step starts', async () => {
    const first = renderer.playStep(SAMPLE_STEPS[0], 'dark');
    const second = renderer.playStep(SAMPLE_STEPS[1], 'dark');
    expect(await first).toBe(false);
    expect(await second).toBe(true);
  });

  it('scales the canvas to the width it is given', () => {
    renderer.resize(400);
    expect(renderer.scale()).toBe(0.5);
  });

  it('plays nothing once destroyed', async () => {
    renderer.destroy();
    expect(await renderer.playStep(SAMPLE_STEPS[0], 'dark')).toBe(false);
  });
});
