package com.ascorp.prepai.generation.validation.rule;

import static com.ascorp.prepai.generation.validation.ValidLessons.validJson;
import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import com.ascorp.prepai.generation.validation.model.ValidationResult;
import org.junit.jupiter.api.Test;

class ParseRuleTest {

	private final ParseRule rule = new ParseRule();

	@Test
	void parsesPlainLessonJson() {
		assertThat(rule.parse(validJson()).lesson().title()).isEqualTo("Projectile Motion");
	}

	@Test
	void stripsMarkdownFencesBeforeParsing() {
		String fenced = "```json\n" + validJson() + "\n```";

		assertThat(rule.parse(fenced).lesson().title()).isEqualTo("Projectile Motion");
	}

	@Test
	void mapsTheOutOfScopeAnswerToItsOwnResult() {
		ValidationResult result = rule.parse("{\"error\": \"OUT_OF_SCOPE\"}");

		assertThat(result.outOfScope()).isTrue();
		assertThat(result.lesson()).isNull();
	}

	@Test
	void reportsProseAsAParseFailure() {
		assertParseFailure("Here is your lesson: steps are below.");
	}

	@Test
	void reportsAJsonArrayAsAParseFailure() {
		assertParseFailure("[1, 2, 3]");
	}

	@Test
	void reportsNullJsonAsAParseFailure() {
		assertParseFailure("null");
	}

	@Test
	void reportsMissingTextAsAParseFailure() {
		assertParseFailure(null);
	}

	private void assertParseFailure(String raw) {
		ValidationResult result = rule.parse(raw);

		assertThat(result.lesson()).isNull();
		assertThat(result.errors()).extracting(ValidationError::code).containsExactly(ValidationCode.PARSE_FAILED);
	}
}
