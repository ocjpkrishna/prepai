package com.ascorp.prepai.account.auth.model.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class RegisterRequestTest {

	private static final int BCRYPT_MAX_BYTES = 72;
	private static final int BYTES_PER_DEVANAGARI_LETTER = 3;
	private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

	@Test
	void aPasswordOfFewCharactersButOverBcryptsByteLimitIsRejected() {
		String password = "अ".repeat(BCRYPT_MAX_BYTES / BYTES_PER_DEVANAGARI_LETTER + 1);

		assertThat(validator.validate(requestWith(password))).isNotEmpty();
	}

	@Test
	void aPasswordWithinTheByteLimitIsAccepted() {
		String password = "अ".repeat(BCRYPT_MAX_BYTES / BYTES_PER_DEVANAGARI_LETTER);

		assertThat(validator.validate(requestWith(password))).isEmpty();
	}

	private static RegisterRequest requestWith(String password) {
		return new RegisterRequest("asha@example.com", password, "Asha", LocalDate.of(2000, 1, 1), true, null);
	}
}
