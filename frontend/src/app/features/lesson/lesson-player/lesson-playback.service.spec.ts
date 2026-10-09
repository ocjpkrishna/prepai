import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { MOCK_FULL_LESSON } from '../../../core/mock/mock-lesson';
import { TtsService } from '../../../core/services/tts.service';
import { LessonPlaybackService } from './lesson-playback.service';

describe('LessonPlaybackService', () => {
  let playback: LessonPlaybackService;
  let available: ReturnType<typeof signal<boolean>>;
  let speakResolvers: ((ok: boolean) => void)[];
  let tts: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    vi.useFakeTimers();
    available = signal(true);
    speakResolvers = [];
    tts = {
      prefetch: vi.fn(),
      speak: vi.fn(() => new Promise<boolean>((resolve) => speakResolvers.push(resolve))),
      pause: vi.fn(),
      resume: vi.fn(),
      stop: vi.fn(),
      setSpeed: vi.fn(),
      retry: vi.fn(),
    };
    TestBed.configureTestingModule({
      providers: [LessonPlaybackService, { provide: TtsService, useValue: { ...tts, available } }],
    });
    playback = TestBed.inject(LessonPlaybackService);
    playback.load(MOCK_FULL_LESSON);
  });

  afterEach(() => {
    playback.stop();
    vi.useRealTimers();
  });

  const tick = async (): Promise<void> => {
    await vi.advanceTimersByTimeAsync(0);
  };

  it('starts narration with the step and waits for the slower of narration and canvas', async () => {
    void playback.start();
    await tick();
    expect(tts['speak']).toHaveBeenCalledTimes(1);
    expect(playback.currentStep()?.stepNumber).toBe(1);
    speakResolvers[0](true);
    await tick();
    expect(playback.stepIndex()).toBe(0);
    playback.onCanvasComplete();
    await tick();
    expect(playback.stepIndex()).toBe(1);
  });

  it('lets narration finish when the canvas is done first', async () => {
    void playback.start();
    await tick();
    playback.onCanvasComplete();
    await tick();
    expect(playback.stepIndex()).toBe(0);
    speakResolvers[0](true);
    await tick();
    expect(playback.stepIndex()).toBe(1);
  });

  it('prefetches the next narration, and the summary after the last step', async () => {
    void playback.start();
    await tick();
    expect(tts['prefetch']).toHaveBeenCalledWith(MOCK_FULL_LESSON.steps[1].narration, 'en-IN');
    expect(tts['prefetch']).toHaveBeenCalledWith(MOCK_FULL_LESSON.summary.narration, 'en-IN');
  });

  it('plays the whole lesson in silent mode when the voice fails', async () => {
    tts['speak'].mockImplementation(async () => {
      available.set(false);
      return false;
    });
    void playback.start();
    await tick();
    for (const step of MOCK_FULL_LESSON.steps) {
      await vi.advanceTimersByTimeAsync(60_000);
      playback.onCanvasComplete();
      expect(playback.caption()).toBe(step.narration);
      await tick();
    }
    await vi.advanceTimersByTimeAsync(60_000);
    expect(playback.phase()).toBe('done');
  });

  it('reveals captions on a timer that follows speed and pause', async () => {
    available.set(false);
    void playback.start();
    await tick();
    await vi.advanceTimersByTimeAsync(1000);
    const normal = playback.captionProgress();
    expect(normal).toBeGreaterThan(0);
    playback.pause();
    await vi.advanceTimersByTimeAsync(5000);
    expect(playback.captionProgress()).toBe(normal);
    playback.resume();
    playback.setSpeed(2);
    await vi.advanceTimersByTimeAsync(1000);
    expect(playback.captionProgress()).toBeGreaterThan(normal * 2.5);
  });

  it('passes pause, resume and speed to the voice', () => {
    playback.pause();
    playback.resume();
    playback.setSpeed(1.5);
    expect(tts['pause']).toHaveBeenCalled();
    expect(tts['resume']).toHaveBeenCalled();
    expect(tts['setSpeed']).toHaveBeenCalledWith(1.5);
    expect(playback.speed()).toBe(1.5);
  });

  it('highlights the word the progress has reached', async () => {
    void playback.start();
    await tick();
    const words = playback.words().length;
    const progress = tts['speak'].mock.calls[0][2] as (fraction: number) => void;
    progress(0.5);
    expect(playback.activeWord()).toBe(Math.floor(words / 2));
  });

  it('stops everything on stop()', async () => {
    void playback.start();
    await tick();
    playback.stop();
    expect(tts['stop']).toHaveBeenCalled();
    speakResolvers[0](true);
    await tick();
    expect(playback.stepIndex()).toBe(0);
  });
});
