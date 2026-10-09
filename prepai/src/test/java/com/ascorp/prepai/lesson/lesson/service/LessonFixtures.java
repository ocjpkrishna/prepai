package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.model.enums.Difficulty;
import com.ascorp.prepai.common.model.enums.Exam;
import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.validation.ValidLessons;
import java.util.UUID;

public final class LessonFixtures {

	public static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	public static final UUID LESSON_ID = UUID.fromString("5b1c2d3e-4f50-4a61-8b72-9c83d94e5f06");

	private LessonFixtures() {
	}

	public static LessonRequest request(LessonRequest.Type type, String text, String imageBase64) {
		return new LessonRequest(type, Subject.PHYSICS, Exam.JEE_MAIN, new LessonRequest.Input(text, imageBase64),
				Difficulty.MEDIUM, Language.EN);
	}

	public static LessonRequest problem() {
		return request(LessonRequest.Type.PROBLEM, "A ball is thrown at 20 m/s.", null);
	}

	public static LessonResponse lesson() {
		return ValidLessons.valid();
	}
}
