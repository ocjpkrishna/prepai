import { Point } from '../../../core/models/canvas-action.model';
import { ActionConfig } from './action-config';
import { WhiteboardScene } from './whiteboard-scene';
import { ThemePalette } from './whiteboard.types';

export interface DrawContext {
  /** The action's id: its element id, which FADE_OUT and CLEAR_CANVAS refer to. */
  readonly id: string;
  readonly scene: WhiteboardScene;
  readonly palette: ThemePalette;
}

/**
 * Draws one frame of one action at `progress` (0 to 1, eased) and returns where the pen tip is,
 * or null when there is no tip. A drawer throws ActionConfigError if the action cannot be drawn.
 */
export type Drawer = (context: DrawContext, config: ActionConfig, progress: number) => Point | null;
