package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.generation.imageextract.model.SanitizedImage;
import com.ascorp.prepai.generation.llm.model.LlmImage;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/** The vision prompt of spec 3.1.1: the reader's rules come from a resource, the photo goes as media. */
final class ExtractionPrompt {

	private static final String USER_TEXT = "Read the problem in the photo and reply with the JSON object only.";
	private static final String SYSTEM_TEXT = read(new ClassPathResource("prompts/image-extract-system-prompt.txt"));

	private ExtractionPrompt() {
	}

	static LlmPrompt forPhoto(SanitizedImage image) {
		LlmImage photo = new LlmImage(image.format().mimeType(), image.bytes());
		return new LlmPrompt(SYSTEM_TEXT, USER_TEXT, photo);
	}

	static String read(Resource resource) {
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Image prompt is missing: " + resource.getDescription(), e);
		}
	}
}
