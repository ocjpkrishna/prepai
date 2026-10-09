import { CanvasAction, Point } from './canvas-action.model';

/** The lesson contract from spec 3.2. */
export interface LessonResponse {
  lessonId: string;
  title: string;
  subject: string;
  topic: string;
  difficulty: string;
  totalSteps: number;
  estimatedDurationSeconds: number;
  steps: LessonStep[];
  summary: LessonSummary;
  masteryCheck: MasteryCheck;
}

export interface LessonStep {
  stepNumber: number;
  title: string;
  narration: string;
  canvas: LessonCanvas;
  equations: LessonEquation[];
}

export interface LessonCanvas {
  actions: CanvasAction[];
}

export interface LessonEquation {
  latex: string;
  highlight: boolean;
  position: Point;
}

export interface LessonSummary {
  narration: string;
  keyResults: KeyResult[];
}

export interface KeyResult {
  label: string;
  value: string;
}

export interface MasteryCheck {
  question: string;
  options: MasteryOption[];
  explanation: string;
}

export interface MasteryOption {
  id: string;
  text: string;
  correct: boolean;
}
