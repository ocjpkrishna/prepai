package com.ascorp.prepai.common.model.lesson;

import com.ascorp.prepai.common.model.enums.Difficulty;
import com.ascorp.prepai.common.model.enums.Exam;
import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Subject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

/** What the student asks for (spec 3.1). */
public record LessonRequest(
		@NotNull Type type,
		@NotNull Subject subject,
		@NotNull Exam exam,
		@Valid @NotNull Input input,
		@NotNull Difficulty difficulty,
		@NotNull Language language) {

	/** TOPIC names a subject area, PROBLEM is a typed question, IMAGE is a photo of one. */
	public enum Type {
		TOPIC,
		PROBLEM,
		IMAGE
	}

	/** The typed text, or the base64 image for IMAGE requests. */
	public record Input(String text, String imageBase64) {
	}
}
