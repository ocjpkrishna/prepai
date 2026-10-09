package com.ascorp.prepai.generation.validation.rule;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.common.model.lesson.LessonResponse.MasteryCheck;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Option;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Step;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Summary;
import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import com.ascorp.prepai.generation.validation.model.ValidationLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/** The structure layer of spec 2.6: step count, numbering, content, key results and the mastery check. */
@Component
public class StructureRule implements ValidationRule {

	private static final int MIN_STEPS = 3;
	private static final int MAX_STEPS = 7;
	private static final int OPTION_COUNT = 4;

	@Override
	public List<ValidationError> check(LessonResponse lesson) {
		List<Step> steps = LessonParts.listOrEmpty(lesson.steps());
		return Stream.of(
						stepCountErrors(steps),
						totalStepsErrors(lesson.totalSteps(), steps),
						stepErrors(steps),
						summaryErrors(lesson.summary()),
						masteryErrors(lesson.masteryCheck()))
				.flatMap(List::stream)
				.toList();
	}

	private List<ValidationError> stepCountErrors(List<Step> steps) {
		if (steps.size() >= MIN_STEPS && steps.size() <= MAX_STEPS) {
			return List.of();
		}
		return List.of(new ValidationError(ValidationLayer.STRUCTURE, ValidationCode.STEP_COUNT,
				"A lesson has %d to %d steps; this one has %d".formatted(MIN_STEPS, MAX_STEPS, steps.size())));
	}

	private List<ValidationError> totalStepsErrors(int totalSteps, List<Step> steps) {
		if (totalSteps == steps.size()) {
			return List.of();
		}
		return List.of(new ValidationError(ValidationLayer.STRUCTURE, ValidationCode.TOTAL_STEPS,
				"totalSteps is %d but the lesson has %d steps".formatted(totalSteps, steps.size())));
	}

	private List<ValidationError> stepErrors(List<Step> steps) {
		return IntStream.range(0, steps.size())
				.boxed()
				.flatMap(index -> stepErrors(index + 1, steps.get(index)).stream())
				.toList();
	}

	private List<ValidationError> stepErrors(int expectedNumber, Step step) {
		if (step == null) {
			return List.of(new ValidationError(ValidationLayer.STRUCTURE, ValidationCode.STEP_CONTENT,
					"Step %d is empty".formatted(expectedNumber)));
		}
		List<ValidationError> errors = new ArrayList<>();
		if (step.stepNumber() != expectedNumber) {
			errors.add(new ValidationError(ValidationLayer.STRUCTURE, ValidationCode.STEP_NUMBER,
					"Step %d has stepNumber %d; numbers run 1 to N in order"
							.formatted(expectedNumber, step.stepNumber())));
		}
		if (!hasContent(step)) {
			errors.add(new ValidationError(ValidationLayer.STRUCTURE, ValidationCode.STEP_CONTENT,
					"Step %d needs a title, a narration and a canvas action or equation"
							.formatted(expectedNumber)));
		}
		return errors;
	}

	private boolean hasContent(Step step) {
		boolean text = isText(step.title()) && isText(step.narration());
		boolean visual = !LessonParts.actions(step).isEmpty() || !LessonParts.equations(step).isEmpty();
		return text && visual;
	}

	private List<ValidationError> summaryErrors(Summary summary) {
		if (summary != null && !LessonParts.listOrEmpty(summary.keyResults()).isEmpty()) {
			return List.of();
		}
		return List.of(new ValidationError(ValidationLayer.STRUCTURE, ValidationCode.KEY_RESULTS,
				"summary.keyResults must list at least one result"));
	}

	private List<ValidationError> masteryErrors(MasteryCheck check) {
		List<Option> options = check == null ? List.of() : LessonParts.listOrEmpty(check.options());
		long correct = options.stream().filter(option -> option != null && option.correct()).count();
		if (options.size() == OPTION_COUNT && correct == 1) {
			return List.of();
		}
		return List.of(new ValidationError(ValidationLayer.STRUCTURE, ValidationCode.MASTERY_OPTIONS,
				"masteryCheck needs exactly %d options and exactly one correct; it has %d options and %d correct"
						.formatted(OPTION_COUNT, options.size(), correct)));
	}

	private boolean isText(String value) {
		return value != null && !value.isBlank();
	}
}
