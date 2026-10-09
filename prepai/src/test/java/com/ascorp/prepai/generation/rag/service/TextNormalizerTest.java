package com.ascorp.prepai.generation.rag.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TextNormalizerTest {

	private final TextNormalizer normalizer = new TextNormalizer();

	@Test
	void lowercasesAndCollapsesWhitespace() {
		assertThat(normalizer.normalize("  A  Block   SLIDES down ")).isEqualTo("a block slides down");
	}

	@Test
	void canonicalisesUnitSpellingsAfterANumber() {
		assertThat(normalizer.normalize("A car at 60 degrees moves 20 metres per second"))
				.isEqualTo("a car at 60deg moves 20m per second");
	}

	@Test
	void leavesTheWordSecondAloneWithoutANumber() {
		assertThat(normalizer.normalize("State Newton's second law")).isEqualTo("state newton's second law");
	}

	@Test
	void redactsEmailAddresses() {
		assertThat(normalizer.normalize("Mail ravi.k@example.com the answer"))
				.isEqualTo("mail [email] the answer");
	}

	@Test
	void redactsIndianMobileNumbers() {
		assertThat(normalizer.normalize("Call +91 98765 43210 about 5 kg"))
				.isEqualTo("call [phone] about 5kg");
	}

	@Test
	void keepsTenDigitQuantitiesThatAreNotMobileNumbers() {
		assertThat(normalizer.normalize("Take G = 1234567890")).isEqualTo("take g = 1234567890");
	}
}
