package com.ascorp.prepai.generation.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.generation.llm.model.LlmImage;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class FakeLessonProviderTest {

	private final FakeLessonProvider provider = new FakeLessonProvider(
			new ClassPathResource("prompts/lesson-example.json"), new ClassPathResource("llm/fake-extraction.json"));

	@Test
	void returnsACannedExtractionForAPhoto() {
		LlmPrompt photo = new LlmPrompt("system", "read", new LlmImage("image/png", new byte[] {1}));

		assertThat(provider.complete(photo).text()).contains("\"confidence\": \"HIGH\"");
	}

	@Test
	void returnsACannedLessonThatPassesEveryValidationLayer() {
		String text = provider.complete(new LlmPrompt("system", "user")).text();

		assertThat(provider.name()).isEqualTo("fake");
		assertThat(LlmTestSupport.validator().validate(text).isValid()).isTrue();
	}

	@Test
	void failsAtStartupWhenTheCannedLessonIsMissing() {
		assertThatThrownBy(() -> new FakeLessonProvider(new ClassPathResource("llm/missing.json"),
				new ClassPathResource("llm/fake-extraction.json")))
				.isInstanceOf(UncheckedIOException.class);
	}
}
