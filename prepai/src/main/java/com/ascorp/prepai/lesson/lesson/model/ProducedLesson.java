package com.ascorp.prepai.lesson.lesson.model;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.llm.model.GenerationMetadata;

/** A lesson ready to store, with its origin. The metadata is present only when the model really ran. */
public record ProducedLesson(LessonResponse lesson, LessonSource source, GenerationMetadata metadata) {

	public static ProducedLesson cached(LessonResponse lesson, LessonSource source) {
		return new ProducedLesson(lesson, source, null);
	}
}
