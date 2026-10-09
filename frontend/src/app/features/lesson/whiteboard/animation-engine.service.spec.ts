import { TestBed } from '@angular/core/testing';
import { AnimationEngineService } from './animation-engine.service';
import { linear } from './easing';

describe('AnimationEngineService', () => {
  let frames: FrameRequestCallback[];
  let engine: AnimationEngineService;

  /** Runs the next queued frame as if it arrived at `time` milliseconds. */
  const nextFrame = (time: number): void => {
    frames.shift()?.(time);
  };

  beforeEach(() => {
    frames = [];
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) => frames.push(callback));
    vi.spyOn(performance, 'now').mockReturnValue(0);
    TestBed.configureTestingModule({ providers: [AnimationEngineService] });
    engine = TestBed.inject(AnimationEngineService);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('reports eased progress frame by frame and resolves when it ends', async () => {
    const onFrame = vi.fn();
    const done = engine.run(100, onFrame, linear);
    nextFrame(50);
    expect(onFrame).toHaveBeenLastCalledWith(0.5);
    nextFrame(120);
    expect(onFrame).toHaveBeenLastCalledWith(1);
    await expect(done).resolves.toBe(true);
  });

  it('runs one frame at full progress for a zero duration', async () => {
    const onFrame = vi.fn();
    const done = engine.run(0, onFrame);
    nextFrame(0);
    expect(onFrame).toHaveBeenCalledOnce();
    expect(onFrame).toHaveBeenCalledWith(1);
    await expect(done).resolves.toBe(true);
  });

  it('stops an older run when a newer one starts', async () => {
    const first = engine.run(100, vi.fn());
    const second = engine.run(100, vi.fn());
    nextFrame(10);
    await expect(first).resolves.toBe(false);
    nextFrame(120);
    await expect(second).resolves.toBe(true);
  });

  it('stops the run on cancel without calling it again', async () => {
    const onFrame = vi.fn();
    const done = engine.run(100, onFrame);
    engine.cancel();
    nextFrame(50);
    expect(onFrame).not.toHaveBeenCalled();
    await expect(done).resolves.toBe(false);
  });

  it('rejects when a frame throws', async () => {
    const done = engine.run(100, () => {
      throw new Error('boom');
    });
    nextFrame(10);
    await expect(done).rejects.toThrow('boom');
  });
});
