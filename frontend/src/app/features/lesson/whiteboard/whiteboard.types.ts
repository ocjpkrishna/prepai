export type WhiteboardTheme = 'dark' | 'light';

export interface ThemePalette {
  readonly ink: string;
  readonly muted: string;
}

/** One problem in one action. The lesson keeps playing (agent 5, task 9). */
export interface RenderWarning {
  readonly actionIndex: number;
  readonly actionType: string;
  readonly message: string;
}
