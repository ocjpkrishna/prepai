export type Subject = 'PHYSICS' | 'CHEMISTRY' | 'MATHEMATICS';
export type Exam = 'JEE_MAIN' | 'JEE_ADVANCED' | 'NEET' | 'CBSE_12' | 'ICSE_10';
export type Difficulty = 'EASY' | 'MEDIUM' | 'HARD';
export type LessonInputType = 'TOPIC' | 'PROBLEM' | 'IMAGE';
export type Confidence = 'HIGH' | 'MEDIUM' | 'LOW';

export interface LessonRequest {
  type: LessonInputType;
  subject: Subject;
  exam: Exam;
  input: { text: string; imageBase64: null };
  difficulty: Difficulty;
  language: 'en' | 'hi';
}

/** Only the fields the input and dashboard pages need; the player (row 17) owns the steps. */
export interface LessonResponse {
  lessonId: string;
  title: string;
  subject: Subject;
  topic: string;
  difficulty: Difficulty;
  totalSteps: number;
  estimatedDurationSeconds: number;
}

export interface LessonSummary {
  lessonId: string;
  title: string;
  subject: Subject;
  createdAt: string;
}

export interface LessonHistoryPage {
  content: LessonSummary[];
  page: number;
  totalPages: number;
}

export interface ExtractResult {
  problemText: string;
  confidence: Confidence;
  hasDiagram: boolean;
}
