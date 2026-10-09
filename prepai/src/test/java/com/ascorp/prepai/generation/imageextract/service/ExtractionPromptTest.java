package com.ascorp.prepai.generation.imageextract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.generation.imageextract.model.ImageFormat;
import com.ascorp.prepai.generation.imageextract.model.SanitizedImage;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.llm.model.LlmPurpose;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ExtractionPromptTest {

	@Test
	void sendsThePhotoAsAnImageExtractionPrompt() {
		LlmPrompt prompt = ExtractionPrompt.forPhoto(new SanitizedImage(ImageFormat.WEBP, new byte[] {1}));

		assertThat(prompt.purpose()).isEqualTo(LlmPurpose.IMAGE_EXTRACT);
		assertThat(prompt.image().mimeType()).isEqualTo("image/webp");
		assertThat(prompt.system()).contains("\"confidence\"");
	}

	@Test
	void failsWhenThePromptResourceIsMissing() {
		assertThatThrownBy(() -> ExtractionPrompt.read(new ClassPathResource("prompts/missing.txt")))
				.isInstanceOf(UncheckedIOException.class);
	}
}
