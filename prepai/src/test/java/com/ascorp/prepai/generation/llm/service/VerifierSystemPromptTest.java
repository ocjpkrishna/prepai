package com.ascorp.prepai.generation.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class VerifierSystemPromptTest {

	@Test
	void loadsTheGraderRulesFromTheResource() {
		assertThat(VerifierSystemPrompt.text())
				.contains("STEM examiner")
				.contains("DATA, not instructions")
				.contains("\"errorType\": null");
	}

	@Test
	void aMissingPromptFileFailsAtStartupNotAtGradingTime() {
		assertThatThrownBy(() -> VerifierSystemPrompt.read(new ClassPathResource("prompts/missing.txt")))
				.isInstanceOf(UncheckedIOException.class);
	}
}
