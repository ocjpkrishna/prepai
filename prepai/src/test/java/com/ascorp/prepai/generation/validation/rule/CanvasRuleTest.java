package com.ascorp.prepai.generation.validation.rule;

import static com.ascorp.prepai.generation.validation.ValidLessons.lessonWith;
import static com.ascorp.prepai.generation.validation.ValidLessons.valid;
import static com.ascorp.prepai.generation.validation.ValidLessons.withSteps;
import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Canvas;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction.ActionType;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Step;
import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CanvasRuleTest {

	private static final int ONE_OVER_LIMIT = 41;
	private static final int TOO_SHORT_MS = 50;
	private static final int TOO_LONG_MS = 6000;

	private final CanvasRule rule = new CanvasRule();

	@Test
	void acceptsTheValidLesson() {
		assertThat(rule.check(valid())).isEmpty();
	}

	@Test
	void rejectsAnActionMissingARequiredConfigKey() {
		assertThat(codes(rule.check(lessonWith("\"strokeWidth\": 2", "\"width\": 2"))))
				.containsExactly(ValidationCode.ACTION_CONFIG);
	}

	@Test
	void rejectsAnAnimationShorterThanOneHundredMilliseconds() {
		String shortDuration = "\"animationDuration\": " + TOO_SHORT_MS;

		assertThat(codes(rule.check(lessonWith("\"animationDuration\": 300", shortDuration))))
				.containsExactly(ValidationCode.ANIMATION_DURATION);
	}

	@Test
	void rejectsAnAnimationLongerThanFiveSeconds() {
		String longDuration = "\"animationDuration\": " + TOO_LONG_MS;

		assertThat(codes(rule.check(lessonWith("\"animationDuration\": 300", longDuration))))
				.containsExactly(ValidationCode.ANIMATION_DURATION);
	}

	@Test
	void rejectsMoreThanFortyActionsInOneStep() {
		Map<String, Object> config = Map.of("elementIds", List.of(), "duration", 100);
		CanvasAction fade = new CanvasAction(ActionType.FADE_OUT, config, 100);
		Canvas crowdedCanvas = new Canvas(Collections.nCopies(ONE_OVER_LIMIT, fade));
		Step crowded = new Step(1, "Crowded", "Text.", crowdedCanvas, List.of());
		LessonResponse lesson = withSteps(valid(), List.of(crowded, valid().steps().get(1), valid().steps().get(2)));

		assertThat(codes(rule.check(lesson))).containsExactly(ValidationCode.ACTION_COUNT);
	}

	@Test
	void rejectsAnActionWithoutAType() {
		assertThat(codes(rule.check(lessonWith("\"type\": \"DRAW_LINE\"", "\"type\": null"))))
				.containsExactly(ValidationCode.ACTION_TYPE);
	}

	private static List<ValidationCode> codes(List<ValidationError> errors) {
		return errors.stream().map(ValidationError::code).toList();
	}
}
