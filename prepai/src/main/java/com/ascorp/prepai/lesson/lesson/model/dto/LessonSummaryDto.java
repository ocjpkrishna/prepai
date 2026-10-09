package com.ascorp.prepai.lesson.lesson.model.dto;

import java.time.Instant;
import java.util.UUID;

/** One row of the lesson history (spec 4.2). */
public record LessonSummaryDto(UUID lessonId, String title, String subject, String topic, String exam,
		String difficulty, Short rating, Instant createdAt) {
}
