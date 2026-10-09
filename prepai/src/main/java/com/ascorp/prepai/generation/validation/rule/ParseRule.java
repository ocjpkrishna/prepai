package com.ascorp.prepai.generation.validation.rule;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import com.ascorp.prepai.generation.validation.model.ValidationLayer;
import com.ascorp.prepai.generation.validation.model.ValidationResult;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The parse layer of spec 2.6: the text must be JSON that deserializes into the lesson contract, or the
 * out-of-scope answer of spec 6.1. It runs before the lesson rules, so it is not a {@link ValidationRule}.
 */
@Component
public class ParseRule {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final Pattern MARKDOWN_FENCE = Pattern.compile("^\\s*```[a-zA-Z]*\\s*|\\s*```\\s*$");
	private static final String OUT_OF_SCOPE = "OUT_OF_SCOPE";
	private static final String PARSE_MESSAGE = "The response is not JSON that matches the lesson contract (spec 3.2)";

	/** Reads the model's text: a lesson, an out-of-scope answer, or a parse failure. */
	public ValidationResult parse(String raw) {
		String json = MARKDOWN_FENCE.matcher(Objects.toString(raw, "")).replaceAll("");
		try {
			if (isOutOfScope(json)) {
				return ValidationResult.outOfScopeAnswer();
			}
			LessonResponse lesson = JSON.readValue(json, LessonResponse.class);
			return lesson == null ? parseFailure() : ValidationResult.parsed(lesson);
		} catch (JacksonException e) {
			return parseFailure();
		}
	}

	private boolean isOutOfScope(String json) {
		Map<?, ?> body = JSON.readValue(json, Map.class);
		return body != null && OUT_OF_SCOPE.equals(body.get("error"));
	}

	private ValidationResult parseFailure() {
		return ValidationResult.failed(
				new ValidationError(ValidationLayer.PARSE, ValidationCode.PARSE_FAILED, PARSE_MESSAGE));
	}
}
