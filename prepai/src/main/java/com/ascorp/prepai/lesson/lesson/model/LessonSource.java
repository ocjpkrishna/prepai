package com.ascorp.prepai.lesson.lesson.model;

/** Where a served lesson came from (spec 5.1 `source`, and the `source` tag of `prepai_lesson_generated_total`). */
public enum LessonSource {
	LLM,
	RAG_CACHE,
	REDIS_CACHE
}
