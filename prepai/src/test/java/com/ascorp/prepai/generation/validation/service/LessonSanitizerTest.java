package com.ascorp.prepai.generation.validation.service;

import static com.ascorp.prepai.generation.validation.ValidLessons.lessonWith;
import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import org.junit.jupiter.api.Test;

class LessonSanitizerTest {

	private final LessonSanitizer sanitizer = new LessonSanitizer();

	@Test
	void stripsTagsAndScriptWithItsContent() {
		LessonResponse lesson = lessonWith("\"title\": \"Projectile Motion\"",
				"\"title\": \"<b>Projectile</b> Motion<script>alert(1)</script>\"");

		assertThat(sanitizer.sanitize(lesson).title()).isEqualTo("Projectile Motion");
	}

	@Test
	void keepsPlainTextWithAmpersandsAndLessThanSigns() {
		LessonResponse lesson = lessonWith("Break the velocity into components.", "Speed a < b & c.");

		assertThat(sanitizer.sanitize(lesson).steps().getFirst().narration()).isEqualTo("Speed a < b & c.");
	}

	@Test
	void stripsHtmlInsideCanvasConfig() {
		LessonResponse lesson = lessonWith("\"label\": \"u\"", "\"label\": \"<i>u</i>\"");

		Object label = sanitizer.sanitize(lesson).steps().getFirst().canvas().actions().get(1).config().get("label");
		assertThat(label).isEqualTo("u");
	}
}
