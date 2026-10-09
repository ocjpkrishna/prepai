import { Difficulty, Exam } from '../../../core/models/lesson.model';

export const MAX_TEXT_LENGTH = 2000;

export const EXAM_OPTIONS: { value: Exam; label: string }[] = [
  { value: 'JEE_MAIN', label: 'JEE Main' },
  { value: 'JEE_ADVANCED', label: 'JEE Advanced' },
  { value: 'NEET', label: 'NEET' },
  { value: 'CBSE_12', label: 'CBSE Class 12' },
  { value: 'ICSE_10', label: 'ICSE Class 10' },
];

export const DIFFICULTY_OPTIONS: { value: Difficulty; label: string }[] = [
  { value: 'EASY', label: 'Easy' },
  { value: 'MEDIUM', label: 'Medium' },
  { value: 'HARD', label: 'Hard' },
];
