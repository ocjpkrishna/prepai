package com.ascorp.prepai.generation.validation.rule;

import static com.ascorp.prepai.generation.validation.ValidLessons.lessonWith;
import static com.ascorp.prepai.generation.validation.ValidLessons.valid;
import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import java.util.List;
import org.junit.jupiter.api.Test;

class TextSafetyRuleTest {

	private static final int ONE_OVER_NARRATION_LIMIT = 601;

	private final TextSafetyRule rule = new TextSafetyRule();

	@Test
	void acceptsSpeakableNarration() {
		assertThat(rule.check(valid())).isEmpty();
	}

	@Test
	void rejectsMarkupCharactersInStepNarration() {
		assertThat(codes(rule.check(lessonWith("Break the velocity into components.", "Use x^2 here."))))
				.containsExactly(ValidationCode.NARRATION_MARKUP);
	}

	@Test
	void rejectsMarkupCharactersInTheSummaryNarration() {
		assertThat(codes(rule.check(lessonWith("So the range is twenty root three metres.", "So R = 2^3."))))
				.containsExactly(ValidationCode.NARRATION_MARKUP);
	}

	@Test
	void rejectsNarrationLongerThanSixHundredCharacters() {
		String tooLong = "a".repeat(ONE_OVER_NARRATION_LIMIT);

		assertThat(codes(rule.check(lessonWith("Break the velocity into components.", tooLong))))
				.containsExactly(ValidationCode.NARRATION_LENGTH);
	}

	@Test
	void rejectsADeniedLatexCommand() {
		assertThat(codes(rule.check(lessonWith("R = u_x * T", "\\\\input{notes}"))))
				.containsExactly(ValidationCode.LATEX_DENYLIST);
	}

	private static List<ValidationCode> codes(List<ValidationError> errors) {
		return errors.stream().map(ValidationError::code).toList();
	}
}
