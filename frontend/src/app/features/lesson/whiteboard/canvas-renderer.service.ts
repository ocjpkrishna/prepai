import { Injectable, inject, signal } from '@angular/core';
import { Layer, Stage } from './konva-shapes';
import { Subject } from 'rxjs';
import { LessonStep } from '../../../core/models/lesson.model';
import { isKnownActionType } from '../../../core/models/canvas-action.model';
import { ActionConfig, RawRecord, isFiniteNumber, isRecord } from './action-config';
import { AnimationEngineService } from './animation-engine.service';
import { DrawContext, Drawer } from './draw-context';
import { easeInOut, linear } from './easing';
import { EquationRendererService } from './equation-renderer.service';
import { clamp } from './geometry';
import { DRAWERS } from './drawers/drawer-registry';
import {
  CANVAS_HEIGHT,
  CANVAS_WIDTH,
  DEFAULT_ACTION_DURATION_MS,
  MAX_ACTION_DURATION_MS,
  STEP_FADE_MS,
  THEME_PALETTES,
} from './whiteboard.constants';
import { RenderWarning, ThemePalette, WhiteboardTheme } from './whiteboard.types';
import { WhiteboardScene } from './whiteboard-scene';

interface PlannedAction {
  index: number;
  type: string;
  id: string;
  duration: number;
  config: ActionConfig;
  drawer: Drawer;
}

/**
 * Plays a lesson step on the board: fades the previous step out, then draws each action in turn.
 * Bad data never throws out of this service. The action is skipped, a warning is emitted and the
 * lesson carries on.
 */
@Injectable()
export class CanvasRendererService {
  readonly warnings = new Subject<RenderWarning>();
  readonly scale = signal(1);
  private readonly animation = inject(AnimationEngineService);
  private readonly equations = inject(EquationRendererService);
  private stage: Stage | null = null;
  private scene: WhiteboardScene | null = null;
  private observer: ResizeObserver | null = null;
  private palette: ThemePalette = THEME_PALETTES.dark;
  private usedIds = new Set<string>();

  attach(stageHost: HTMLDivElement, latexHost: HTMLDivElement): void {
    if (this.stage) {
      return;
    }
    this.stage = new Stage({ container: stageHost, width: CANVAS_WIDTH, height: CANVAS_HEIGHT });
    const layer = new Layer();
    this.stage.add(layer);
    this.scene = new WhiteboardScene(layer, latexHost, this.equations);
    if (typeof ResizeObserver !== 'undefined') {
      this.observer = new ResizeObserver((entries) => this.resize(entries[0].contentRect.width));
      this.observer.observe(stageHost);
    }
    this.resize(stageHost.clientWidth);
  }

  /** Keeps the 800 by 500 canvas at the given width. Logical coordinates stay the same. */
  resize(width: number): void {
    const scale = width > 0 ? width / CANVAS_WIDTH : 1;
    this.scale.set(scale);
    this.stage?.width(CANVAS_WIDTH * scale);
    this.stage?.height(CANVAS_HEIGHT * scale);
    this.stage?.scale({ x: scale, y: scale });
    this.scene?.setScale(scale);
  }

  /** Resolves true when the step played to the end, false when a newer step or destroy() stopped it. */
  async playStep(step: LessonStep, theme: WhiteboardTheme): Promise<boolean> {
    const scene = this.scene;
    if (!scene) {
      return false;
    }
    this.palette = THEME_PALETTES[theme];
    this.usedIds = new Set<string>();
    this.animation.cancel();
    if (!(await this.clearPrevious(scene))) {
      return false;
    }
    return this.playActions(scene, this.actionsOf(step));
  }

  destroy(): void {
    this.animation.cancel();
    this.observer?.disconnect();
    this.observer = null;
    this.stage?.destroy();
    this.stage = null;
    this.scene = null;
    this.warnings.complete();
  }

  private async clearPrevious(scene: WhiteboardScene): Promise<boolean> {
    if (!scene.hasContent()) {
      scene.setLayerOpacity(1);
      return true;
    }
    const faded = await this.animation.run(STEP_FADE_MS, (progress) => scene.setLayerOpacity(1 - progress), linear);
    if (!faded) {
      return false;
    }
    scene.clearAll();
    scene.setLayerOpacity(1);
    scene.draw();
    return true;
  }

  private actionsOf(step: LessonStep): unknown[] {
    const actions = step.canvas?.actions;
    if (Array.isArray(actions)) {
      return actions;
    }
    this.warn(-1, 'STEP', 'canvas has no action list, nothing drawn');
    return [];
  }

  private async playActions(scene: WhiteboardScene, actions: unknown[]): Promise<boolean> {
    for (const [index, raw] of actions.entries()) {
      if (!(await this.playAction(scene, raw, index))) {
        return false;
      }
    }
    return true;
  }

  private async playAction(scene: WhiteboardScene, raw: unknown, index: number): Promise<boolean> {
    const action = this.plan(raw, index);
    if (!action) {
      return true;
    }
    try {
      return await this.animation.run(action.duration, (progress) => this.drawFrame(scene, action, progress), easeInOut);
    } catch (error) {
      scene.remove([action.id]);
      this.warn(index, action.type, `could not be drawn, skipped: ${describe(error)}`);
      return true;
    } finally {
      scene.penTo(null, this.palette.ink);
      new Set(action.config.notes).forEach((note) => this.warn(index, action.type, note));
    }
  }

  private plan(raw: unknown, index: number): PlannedAction | null {
    if (!isRecord(raw)) {
      this.warn(index, 'UNKNOWN', 'action is not an object, skipped');
      return null;
    }
    const type = raw['type'];
    if (typeof type !== 'string' || !isKnownActionType(type)) {
      this.warn(index, String(type), 'unknown action type, skipped');
      return null;
    }
    const config = new ActionConfig(isRecord(raw['config']) ? raw['config'] : {});
    return {
      index,
      type,
      id: this.uniqueId(config.text('id', '')),
      duration: durationOf(raw),
      config,
      drawer: DRAWERS[type],
    };
  }

  private uniqueId(requested: string): string {
    let id = requested === '' ? `action-${this.usedIds.size}` : requested;
    while (this.usedIds.has(id)) {
      id = `${id}-${this.usedIds.size}`;
    }
    this.usedIds.add(id);
    return id;
  }

  private drawFrame(scene: WhiteboardScene, action: PlannedAction, progress: number): void {
    const context: DrawContext = { id: action.id, scene, palette: this.palette };
    scene.resetGroup(action.id);
    const tip = action.drawer(context, action.config, progress);
    scene.penTo(tip, this.palette.ink);
    scene.draw();
  }

  private warn(actionIndex: number, actionType: string, message: string): void {
    console.warn(`[whiteboard] action ${actionIndex} (${actionType}): ${message}`);
    this.warnings.next({ actionIndex, actionType, message });
  }
}

function durationOf(raw: RawRecord): number {
  const duration = raw['animationDuration'];
  return isFiniteNumber(duration) ? clamp(duration, 0, MAX_ACTION_DURATION_MS) : DEFAULT_ACTION_DURATION_MS;
}

function describe(error: unknown): string {
  return error instanceof Error ? error.message : 'unexpected error';
}
