package com.ascorp.prepai.generation.validation.service;

import static com.ascorp.prepai.generation.validation.ValidLessons.validJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import com.ascorp.prepai.generation.validation.model.ValidationResult;
import com.ascorp.prepai.generation.validation.rule.CanvasBoundsRule;
import com.ascorp.prepai.generation.validation.rule.CanvasRule;
import com.ascorp.prepai.generation.validation.rule.ParseRule;
import com.ascorp.prepai.generation.validation.rule.StructureRule;
import com.ascorp.prepai.generation.validation.rule.TextSafetyRule;
import java.util.List;
import org.junit.jupiter.api.Test;

class LessonValidatorTest {

	private final LessonValidator validator = new LessonValidator(new ParseRule(), new LessonSanitizer(),
			List.of(new StructureRule(), new CanvasRule(), new CanvasBoundsRule(), new TextSafetyRule()));

	@Test
	void acceptsTheValidLesson() {
		assertThat(validator.validate(validJson()).isValid()).isTrue();
	}

	@Test
	void acceptsALessonWrappedInMarkdownFences() {
		assertThat(validator.validate("```json\n" + validJson() + "\n```").isValid()).isTrue();
	}

	@Test
	void reportsProseAsAParseFailureWithNoLesson() {
		ValidationResult result = validator.validate("Sorry, I cannot help with that.");

		assertThat(result.lesson()).isNull();
		assertThat(result.isValid()).isFalse();
		assertThat(codes(result.errors())).containsExactly(ValidationCode.PARSE_FAILED);
	}

	@Test
	void returnsTheLessonWithEveryRuleErrorSoTheModelCanRepairIt() {
		ValidationResult result = validator.validate(validJson().replace("\"totalSteps\": 3", "\"totalSteps\": 4"));

		assertThat(result.lesson()).isNotNull();
		assertThat(result.isValid()).isFalse();
		assertThat(codes(result.errors())).containsExactly(ValidationCode.TOTAL_STEPS);
	}

	@Test
	void sanitisesTheTextBeforeTheRulesCheckIt() {
		String json = validJson().replace("Break the velocity into components.", "<b>Break</b> the velocity.");

		ValidationResult result = validator.validate(json);

		assertThat(result.isValid()).isTrue();
		assertThat(result.lesson().steps().getFirst().narration()).isEqualTo("Break the velocity.");
	}

	@Test
	void passesTheOutOfScopeAnswerThrough() {
		ValidationResult result = validator.validate("{\"error\": \"OUT_OF_SCOPE\"}");

		assertThat(result.outOfScope()).isTrue();
		assertThat(result.isValid()).isFalse();
	}

	private static List<ValidationCode> codes(List<ValidationError> errors) {
		return errors.stream().map(ValidationError::code).toList();
	}
}
