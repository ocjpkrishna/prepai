package com.ascorp.prepai.generation.validation.rule;

import static com.ascorp.prepai.generation.validation.ValidLessons.lessonWith;
import static com.ascorp.prepai.generation.validation.ValidLessons.valid;
import static com.ascorp.prepai.generation.validation.ValidLessons.withSteps;
import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.common.model.lesson.LessonResponse.Canvas;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction.ActionType;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Step;
import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanvasBoundsRuleTest {

	private final CanvasBoundsRule rule = new CanvasBoundsRule();

	@Test
	void acceptsPointsInsideTheCanvasAndPanel() {
		assertThat(rule.check(valid())).isEmpty();
	}

	@Test
	void rejectsAPointOutsideTheCanvas() {
		assertThat(codes(rule.check(lessonWith("\"position\": {\"x\": 275, \"y\": 100}",
				"\"position\": {\"x\": 801, \"y\": 100}")))).contains(ValidationCode.COORDINATE_BOUNDS);
	}

	@Test
	void rejectsADrawingBeyondXFiveHundred() {
		assertThat(codes(rule.check(lessonWith("\"to\": {\"x\": 450, \"y\": 320}",
				"\"to\": {\"x\": 600, \"y\": 320}")))).containsExactly(ValidationCode.DRAWING_BOUNDS);
	}

	@Test
	void allowsTextBeyondXFiveHundred() {
		Map<String, Object> config = Map.of("position", Map.of("x", 600, "y", 100), "text", "Note",
				"fontSize", 14, "color", "#000000");
		CanvasAction label = new CanvasAction(ActionType.WRITE_TEXT, config, 300);
		Step step = new Step(3, "Range", "Text.", new Canvas(List.of(label)), List.of());

		assertThat(rule.check(withSteps(valid(), List.of(valid().steps().get(0), valid().steps().get(1), step))))
				.isEmpty();
	}

	@Test
	void rejectsAnEquationOutsideThePanel() {
		assertThat(codes(rule.check(lessonWith("\"position\": {\"x\": 520, \"y\": 200}",
				"\"position\": {\"x\": 300, \"y\": 200}")))).containsExactly(ValidationCode.EQUATION_POSITION);
	}

	private static List<ValidationCode> codes(List<ValidationError> errors) {
		return errors.stream().map(ValidationError::code).toList();
	}
}
