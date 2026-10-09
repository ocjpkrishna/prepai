package com.ascorp.prepai.generation.validation.service;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import com.ascorp.prepai.generation.validation.model.ValidationResult;
import com.ascorp.prepai.generation.validation.rule.ParseRule;
import com.ascorp.prepai.generation.validation.rule.ValidationRule;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The single gate every model response passes before it is stored, cached or sent (spec 2.6). Parsing comes first,
 * then the sanitiser, then every {@link ValidationRule} bean, which the error list collects.
 */
@Service
@RequiredArgsConstructor
public class LessonValidator {

	private final ParseRule parseRule;
	private final LessonSanitizer sanitizer;
	private final List<ValidationRule> rules;

	/** Validates the raw model text. The result carries the sanitised lesson and every error found. */
	public ValidationResult validate(String rawJson) {
		ValidationResult parsed = parseRule.parse(rawJson);
		return parsed.lesson() == null ? parsed : checkLesson(sanitizer.sanitize(parsed.lesson()));
	}

	private ValidationResult checkLesson(LessonResponse lesson) {
		List<ValidationError> errors = rules.stream()
				.flatMap(rule -> rule.check(lesson).stream())
				.toList();
		return ValidationResult.checked(lesson, errors);
	}
}
