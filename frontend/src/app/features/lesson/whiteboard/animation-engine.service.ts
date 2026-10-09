import { Injectable } from '@angular/core';
import { clamp } from './geometry';
import { Easing, easeOutCubic } from './easing';

/**
 * Drives one frame loop at a time. A new run, or cancel(), stops the previous one.
 * Resolves true when the run finishes and false when it was stopped.
 */
@Injectable()
export class AnimationEngineService {
  private activeRun = 0;

  run(durationMs: number, onFrame: (progress: number) => void, easing: Easing = easeOutCubic): Promise<boolean> {
    const runId = ++this.activeRun;
    return new Promise<boolean>((resolve, reject) => {
      const start = performance.now();
      const frame = (now: number): void => {
        if (runId !== this.activeRun) {
          resolve(false);
          return;
        }
        const linear = durationMs > 0 ? clamp((now - start) / durationMs, 0, 1) : 1;
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
