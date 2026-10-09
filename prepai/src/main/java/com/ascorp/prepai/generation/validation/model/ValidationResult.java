package com.ascorp.prepai.generation.validation.model;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import java.util.List;

/**
 * The outcome of validating one model response. The lesson is set when the JSON parsed; it is usable only when
 * {@link #isValid()} is true. An out-of-scope answer (spec 6.1) has no lesson and no errors.
 */
public record ValidationResult(LessonResponse lesson, List<ValidationError> errors, boolean outOfScope) {

	public static ValidationResult parsed(LessonResponse lesson) {
		return new ValidationResult(lesson, List.of(), false);
	}

	public static ValidationResult checked(LessonResponse lesson, List<ValidationError> errors) {
		return new ValidationResult(lesson, errors, false);
	}

	public static ValidationResult failed(ValidationError error) {
		return new ValidationResult(null, List.of(error), false);
	}

	public static ValidationResult outOfScopeAnswer() {
		return new ValidationResult(null, List.of(), true);
	}

	public boolean isValid() {
		return lesson != null && errors.isEmpty() && !outOfScope;
	}
}
