package com.ascorp.prepai.generation.validation.rule;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Canvas;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Equation;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Step;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

/** Null-safe views of a lesson's parts: JSON from the model can leave any list empty or null. */
final class LessonParts {

	private LessonParts() {
	}

	static <T> List<T> listOrEmpty(List<T> items) {
		return items == null ? List.of() : items;
	}

	static Stream<Step> steps(LessonResponse lesson) {
		return listOrEmpty(lesson.steps()).stream().filter(Objects::nonNull);
	}

	static List<CanvasAction> actions(Step step) {
		Canvas canvas = step.canvas();
		return canvas == null ? List.of() : listOrEmpty(canvas.actions()).stream().filter(Objects::nonNull).toList();
	}

	static List<Equation> equations(Step step) {
		return listOrEmpty(step.equations()).stream().filter(Objects::nonNull).toList();
	}
}
