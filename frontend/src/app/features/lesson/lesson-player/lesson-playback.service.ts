import { computed, inject, Injectable, signal } from '@angular/core';
import { LessonResponse, LessonStep } from '../../../core/models/lesson.model';
import { TtsService } from '../../../core/services/tts.service';

const NARRATION_LANGUAGE = 'en-IN';
const SILENT_WORD_MS = 350;
const SILENT_MIN_MS = 1500;
const SILENT_TICK_MS = 100;

export type PlayPhase = 'ready' | 'steps' | 'summary' | 'done';

/**
 * Runs one lesson: each step's canvas animation and narration start together and the step ends
 * when both are done. Voice failures switch to a timed caption reveal; the lesson never stops.
 */
@Injectable()
export class LessonPlaybackService {
  readonly phase = signal<PlayPhase>('ready');
  readonly stepIndex = signal(0);
  readonly speed = signal(1);
  readonly paused = signal(false);
  readonly caption = signal('');
  readonly captionProgress = signal(0);
  readonly lesson = signal<LessonResponse | null>(null);

  readonly currentStep = computed<LessonStep | null>(() =>
    this.phase() === 'steps' || this.phase() === 'summary' ? (this.lesson()?.steps[this.stepIndex()] ?? null) : null,
  );
  readonly words = computed(() => this.caption().split(/\s+/).filter(Boolean));
  readonly activeWord = computed(() => Math.min(this.words().length - 1, Math.floor(this.captionProgress() * this.words().length)));

  private readonly tts = inject(TtsService);
  private session = 0;
  private canvasDone: (() => void) | null = null;
  private silentTimer: ReturnType<typeof setInterval> | null = null;
  private silentDone: (() => void) | null = null;

  load(lesson: LessonResponse): void {
    this.stop();
    this.lesson.set(lesson);
    this.phase.set('ready');
    this.stepIndex.set(0);
    this.caption.set('');
  }

  /** Call from a click: the first audio playback needs a user gesture. */
  async start(): Promise<void> {
    const lesson = this.lesson();
    if (!lesson) {
      return;
    }
    const session = this.begin();
    this.phase.set('steps');
    for (const index of lesson.steps.keys()) {
      this.stepIndex.set(index);
      await this.playStep(lesson, index);
      if (session !== this.session) {
        return;
      }
    }
    await this.playSummary(lesson, session);
  }

  onCanvasComplete(): void {
    this.canvasDone?.();
    this.canvasDone = null;
  }

  pause(): void {
    this.paused.set(true);
    this.tts.pause();
  }

  resume(): void {
    this.paused.set(false);
    this.tts.resume();
  }

  setSpeed(speed: number): void {
    this.speed.set(speed);
    this.tts.setSpeed(speed);
  }

  retryVoice(): void {
    this.tts.retry();
  }

  stop(): void {
    this.session += 1;
    this.tts.stop();
    this.clearSilent();
    this.canvasDone?.();
    this.canvasDone = null;
    this.paused.set(false);
  }

  private begin(): number {
    this.stop();
    return this.session;
  }

  private async playStep(lesson: LessonResponse, index: number): Promise<void> {
    const step = lesson.steps[index];
    const canvas = new Promise<void>((resolve) => (this.canvasDone = resolve));
    this.prefetchAfter(lesson, index);
    await Promise.all([canvas, this.narrate(step.narration)]);
  }

  private async playSummary(lesson: LessonResponse, session: number): Promise<void> {
    this.phase.set('summary');
    await this.narrate(lesson.summary.narration);
    if (session === this.session) {
      this.phase.set('done');
    }
  }

  private prefetchAfter(lesson: LessonResponse, index: number): void {
    const next = lesson.steps[index + 1]?.narration ?? lesson.summary.narration;
    this.tts.prefetch(next, NARRATION_LANGUAGE);
    if (index === 0) {
      this.tts.prefetch(lesson.summary.narration, NARRATION_LANGUAGE);
    }
  }

  private async narrate(text: string): Promise<void> {
    this.caption.set(text);
    this.captionProgress.set(0);
    if (this.tts.available()) {
      const played = await this.tts.speak(text, NARRATION_LANGUAGE, (fraction) => this.captionProgress.set(fraction));
      if (played) {
        this.captionProgress.set(1);
        return;
      }
    }
    await this.revealSilently();
  }

  /** Timed caption reveal for when the voice is unavailable; follows pause and speed. */
  private revealSilently(): Promise<void> {
    const total = Math.max(SILENT_MIN_MS, this.words().length * SILENT_WORD_MS);
    let elapsed = this.captionProgress() * total;
    return new Promise<void>((resolve) => {
      this.clearSilent();
      this.silentDone = resolve;
      this.silentTimer = setInterval(() => {
        elapsed += this.paused() ? 0 : SILENT_TICK_MS * this.speed();
        this.captionProgress.set(Math.min(1, elapsed / total));
        if (elapsed >= total) {
          this.clearSilent();
        }
      }, SILENT_TICK_MS);
    });
  }

  private clearSilent(): void {
    if (this.silentTimer !== null) {
      clearInterval(this.silentTimer);
      this.silentTimer = null;
    }
    this.silentDone?.();
    this.silentDone = null;
  }
}
