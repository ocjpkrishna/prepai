import { ThemePalette, WhiteboardTheme } from './whiteboard.types';

export const CANVAS_WIDTH = 800;
export const CANVAS_HEIGHT = 500;
/** The equation panel runs from x 500 to x 800 (agent 5, task 11). */
export const EQUATION_PANEL_X = 500;
export const TEXT_LIMIT = 300;
export const MAX_LIST_ITEMS = 50;

export const DEFAULT_ACTION_DURATION_MS = 600;
export const MAX_ACTION_DURATION_MS = 10000;
export const STEP_FADE_MS = 250;

export const STROKE_WIDTH = 2;
export const MIN_STROKE_WIDTH = 0.5;
export const MAX_STROKE_WIDTH = 20;
export const DASH_PATTERN = [8, 6];
export const ARROW_HEAD_SIZE = 10;
export const LABEL_FONT_SIZE = 14;
export const LABEL_OFFSET = 12;
export const DEFAULT_FONT_SIZE = 16;
export const MIN_FONT_SIZE = 8;
export const MAX_FONT_SIZE = 72;
export const DEFAULT_POINT_RADIUS = 5;
export const MAX_POINT_RADIUS = 50;
export const PEN_RADIUS = 4;
export const HIGHLIGHT_OPACITY = 0.25;
export const MIN_REGION_POINTS = 3;

/** Segments used to trace curves; more segments give a smoother line. */
export const ARC_SEGMENTS = 48;
export const CIRCLE_SEGMENTS = 64;
export const PARABOLA_SEGMENTS = 60;
export const GRAPH_SAMPLES = 200;
export const FULL_TURN_DEGREES = 360;

/** Physics vectors: pixels per unit of magnitude. */
export const PX_PER_UNIT = 10;
export const MAX_MAGNITUDE = 1000;
export const BODY_SIZE = 24;
export const RESISTOR_LENGTH = 20;
export const RESISTOR_THICKNESS = 8;
export const CAPACITOR_GAP = 4;
export const CAPACITOR_PLATE_HALF = 8;
export const BATTERY_LONG_HALF = 10;
export const BATTERY_SHORT_HALF = 5;

export const DEFAULT_GRAPH_BOX = { x: 40, y: 40, width: 440, height: 400 };
export const DEFAULT_X_RANGE: [number, number] = [-10, 10];
export const DEFAULT_Y_RANGE: [number, number] = [-10, 10];

export const THEME_PALETTES: Readonly<Record<WhiteboardTheme, ThemePalette>> = {
  dark: { ink: '#F3F4F6', muted: '#9CA3AF' },
  light: { ink: '#111827', muted: '#6B7280' },
};
