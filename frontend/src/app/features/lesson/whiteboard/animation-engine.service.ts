import { Injectable } from '@angular/core';
import { clamp } from './geometry';
import { Easing, easeOutCubic } from './easing';

/**
 * Drives one frame loop at a time. A new run, or cancel(), stops the previous one.
 * Resolves true when the run finishes and false when it was stopped.
 * The timeline runs on its own clock: setSpeed() scales it and setPaused() freezes it.
 */
@Injectable()
export class AnimationEngineService {
  private activeRun = 0;
  private speed = 1;
  private paused = false;

  setSpeed(speed: number): void {
    this.speed = speed > 0 ? speed : 1;
  }

  setPaused(paused: boolean): void {
    this.paused = paused;
  }

  run(durationMs: number, onFrame: (progress: number) => void, easing: Easing = easeOutCubic): Promise<boolean> {
    const runId = ++this.activeRun;
    return new Promise<boolean>((resolve, reject) => {
      let elapsed = 0;
      let last = performance.now();
      const frame = (now: number): void => {
        if (runId !== this.activeRun) {
          resolve(false);
          return;
        }
        elapsed += this.paused ? 0 : Math.max(0, now - last) * this.speed;
        last = now;
        const linear = durationMs > 0 ? clamp(elapsed / durationMs, 0, 1) : 1;
        try {
          onFrame(easing(linear));
        } catch (error) {
          reject(error);
          return;
        }
        if (linear >= 1) {
          resolve(true);
          return;
        }
        requestAnimationFrame(frame);
      };
      requestAnimationFrame(frame);
    });
  }

  cancel(): void {
    this.activeRun += 1;
  }
}
