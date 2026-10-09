import {
  Component,
  DestroyRef,
  ElementRef,
  afterNextRender,
  computed,
  effect,
  inject,
  input,
  output,
  viewChild,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LessonStep } from '../../../core/models/lesson.model';
import { AnimationEngineService } from './animation-engine.service';
import { CanvasRendererService } from './canvas-renderer.service';
import { EquationRendererService } from './equation-renderer.service';
import { WhiteboardEquationComponent } from './whiteboard-equation.component';
import { CANVAS_WIDTH, EQUATION_PANEL_X } from './whiteboard.constants';
import { RenderWarning, WhiteboardTheme } from './whiteboard.types';

/**
 * Draws one lesson step at a time on a Konva canvas. Equations stack in a column in the
 * panel at the right, starting at the first equation's y. Emits stepComplete when a step ends.
 */
@Component({
  selector: 'app-whiteboard',
  imports: [WhiteboardEquationComponent],
  templateUrl: './whiteboard.component.html',
  styleUrl: './whiteboard.component.scss',
  providers: [AnimationEngineService, EquationRendererService, CanvasRendererService],
  host: { '[class.theme-light]': "theme() === 'light'" },
})
export class WhiteboardComponent {
  readonly step = input<LessonStep | null>(null);
  readonly theme = input<WhiteboardTheme>('dark');
  readonly stepComplete = output<number>();
  readonly renderWarning = output<RenderWarning>();

  private readonly stageHost = viewChild.required<ElementRef<HTMLDivElement>>('stage');
  private readonly latexHost = viewChild.required<ElementRef<HTMLDivElement>>('latex');
  private readonly renderer = inject(CanvasRendererService);

  protected readonly equations = computed(() => this.step()?.equations ?? []);
  protected readonly panelLeft = computed(() => EQUATION_PANEL_X * this.renderer.scale());
  protected readonly panelTop = computed(() => this.firstEquationY() * this.renderer.scale());
  protected readonly panelWidth = computed(() => (CANVAS_WIDTH - EQUATION_PANEL_X) * this.renderer.scale());

  constructor() {
    afterNextRender(() => {
      this.renderer.attach(this.stageHost().nativeElement, this.latexHost().nativeElement);
      void this.play(this.step(), this.theme());
    });
    effect(() => {
      void this.play(this.step(), this.theme());
    });
    this.renderer.warnings.pipe(takeUntilDestroyed()).subscribe((warning) => this.renderWarning.emit(warning));
    inject(DestroyRef).onDestroy(() => this.renderer.destroy());
  }

  private async play(step: LessonStep | null, theme: WhiteboardTheme): Promise<void> {
    if (step === null) {
      return;
    }
    const finished = await this.renderer.playStep(step, theme);
    if (finished) {
      this.stepComplete.emit(step.stepNumber);
    }
  }

  private firstEquationY(): number {
    const y = this.equations()[0]?.position?.y;
    return Number.isFinite(y) ? y : 0;
  }
}
