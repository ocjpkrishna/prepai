import { Injectable } from '@angular/core';
import { MOCK_KEYWORDS, MOCK_LESSONS } from '../mock/mock-lessons';
import { Lesson } from '../models/whiteboard.model';
import { LessonSource, LessonSummary } from './lesson-source';

const THINKING_MS = 700;

@Injectable()
export class MockLessonSource extends LessonSource {
	async get(id: string): Promise<Lesson | undefined> {
		return MOCK_LESSONS.find((lesson) => lesson.lessonId === id);
	}

	async list(): Promise<LessonSummary[]> {
		return MOCK_LESSONS.map(({ lessonId, title, subject, topic, difficulty }) => ({ lessonId, title, subject, topic, difficulty }));
	}

	async generate(prompt: string): Promise<Lesson> {
		await new Promise((resolve) => setTimeout(resolve, THINKING_MS));
		const text = prompt.toLowerCase();
		const hit = Object.keys(MOCK_KEYWORDS).find((word) => text.includes(word));
		return MOCK_LESSONS.find((l) => l.lessonId === (hit ? MOCK_KEYWORDS[hit] : MOCK_LESSONS[0].lessonId))!;
	}
}
