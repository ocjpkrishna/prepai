package com.ascorp.prepai.generation.llm.model;

import com.ascorp.prepai.common.model.lesson.LessonResponse;

/** A lesson that passed every validation layer, with the metadata the lesson module stores. */
public record LessonGenerationResult(LessonResponse lesson, GenerationMetadata metadata) {
}
