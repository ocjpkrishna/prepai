package com.ascorp.prepai.generation.validation.rule;

import static com.ascorp.prepai.generation.validation.ValidLessons.lessonWith;
import static com.ascorp.prepai.generation.validation.ValidLessons.valid;
import static com.ascorp.prepai.generation.validation.ValidLessons.withParts;
import static com.ascorp.prepai.generation.validation.ValidLessons.withSteps;
import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.common.model.lesson.LessonResponse.MasteryCheck;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Option;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Step;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Summary;
import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class StructureRuleTest {

	private static final int EXTRA_STEPS = 5;

	private final StructureRule rule = new StructureRule();

	@Test
	void acceptsTheValidLesson() {
		assertThat(rule.check(valid())).isEmpty();
	}

	@Test
	void rejectsFewerThanThreeSteps() {
		LessonResponse lesson = withSteps(valid(), valid().steps().subList(0, 2));

		assertThat(codes(rule.check(lesson))).containsExactly(ValidationCode.STEP_COUNT);
	}

	@Test
	void rejectsMoreThanSevenSteps() {
		List<Step> steps = valid().steps();
		List<Step> tooMany = new ArrayList<>(steps);
		for (int i = 0; i < EXTRA_STEPS; i++) {
			tooMany.add(steps.getFirst());
		}

		assertThat(codes(rule.check(withSteps(valid(), tooMany)))).contains(ValidationCode.STEP_COUNT);
	}

	@Test
	void rejectsStepNumbersThatAreOutOfOrder() {
		assertThat(codes(rule.check(lessonWith("\"stepNumber\": 2", "\"stepNumber\": 5"))))
				.contains(ValidationCode.STEP_NUMBER);
	}

	@Test
	void rejectsTotalStepsThatDoNotMatchTheStepCount() {
		assertThat(codes(rule.check(lessonWith("\"totalSteps\": 3", "\"totalSteps\": 4"))))
				.containsExactly(ValidationCode.TOTAL_STEPS);
	}

	@Test
	void rejectsABlankTitle() {
		assertThat(codes(rule.check(lessonWith("\"title\": \"Range\"", "\"title\": \" \""))))
				.containsExactly(ValidationCode.STEP_CONTENT);
	}

	@Test
	void rejectsAStepWithNeitherCanvasNorEquations() {
		Step bare = new Step(3, "Range", "Text.", null, null);
		List<Step> steps = List.of(valid().steps().get(0), valid().steps().get(1), bare);

		assertThat(codes(rule.check(withSteps(valid(), steps)))).containsExactly(ValidationCode.STEP_CONTENT);
	}

	@Test
	void rejectsAnEmptyKeyResultsList() {
		Summary summary = new Summary("So the range is twenty root three metres.", List.of());

		assertThat(codes(rule.check(withParts(valid(), valid().steps(), summary, valid().masteryCheck()))))
				.containsExactly(ValidationCode.KEY_RESULTS);
	}

	@Test
	void rejectsAMasteryCheckWithThreeOptions() {
		List<Option> options = List.of(new Option("A", "Up", false), new Option("B", "Same", true),
				new Option("C", "Down", false));
		MasteryCheck mastery = new MasteryCheck("Question?", options, "Because.");

		assertThat(codes(rule.check(withParts(valid(), valid().steps(), valid().summary(), mastery))))
				.containsExactly(ValidationCode.MASTERY_OPTIONS);
	}

	@Test
	void rejectsAMasteryCheckWithTwoCorrectOptions() {
		assertThat(codes(rule.check(lessonWith("\"It cannot be found\", \"correct\": false",
				"\"It cannot be found\", \"correct\": true"))))
				.containsExactly(ValidationCode.MASTERY_OPTIONS);
	}

	private static List<ValidationCode> codes(List<ValidationError> errors) {
		return errors.stream().map(ValidationError::code).toList();
	}
}
