package com.ascorp.prepai.generation.rag.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class NumericSignatureServiceTest {

	private final NumericSignatureService signatures = new NumericSignatureService();

	@Test
	void listsEveryNumberWithItsUnitInOrder() {
		assertThat(signatures.signatureOf("a block at 60deg with 20m/s and 5kg")).contains("60deg|20m/s|5kg");
	}

	@Test
	void differsWhenOnlyTheNumbersDiffer() {
		assertThat(signatures.signatureOf("a block with 20m/s"))
				.isNotEqualTo(signatures.signatureOf("a block with 25m/s"));
	}

	@Test
	void keepsThePowerSignSoExponentsDoNotCollide() {
		assertThat(signatures.signatureOf("charge 10^11 c")).isNotEqualTo(signatures.signatureOf("charge 10^-11 c"));
	}

	@Test
	void keepsTheSignOfANumber() {
		assertThat(signatures.signatureOf("x -3")).isNotEqualTo(signatures.signatureOf("x 3"));
	}

	@Test
	void isEmptyWhenTheSignatureWouldNotFitTheColumn() {
		String longNumbers = "1 ".repeat(300);

		assertThat(signatures.signatureOf(longNumbers)).isEmpty();
	}

	@Test
	void isEmptyStringForAProblemWithoutNumbers() {
		assertThat(signatures.signatureOf("state newton's laws")).contains("");
	}
}
