import { Lesson } from '../models/whiteboard.model';

export interface LessonSummary {
	lessonId: string;
	title: string;
	subject: string;
	topic: string;
	difficulty: string;
}

export abstract class LessonSource {
	abstract get(id: string): Promise<Lesson | undefined>;
	abstract list(): Promise<LessonSummary[]>;
	abstract generate(prompt: string): Promise<Lesson>;
}
