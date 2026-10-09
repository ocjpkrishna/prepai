package com.ascorp.prepai.generation.llm.service;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

/** The verifier's system prompt (spec 8.1), kept as a resource beside the spec 6 prompts. */
final class VerifierSystemPrompt {

	private static final String RESOURCE = "prompts/verifier-system-prompt.txt";
	private static final String TEXT = read(new ClassPathResource(RESOURCE));

	private VerifierSystemPrompt() {
	}

	static String text() {
		return TEXT;
	}

	static String read(Resource resource) {
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Verifier prompt is missing: " + resource.getDescription(), e);
		}
	}
}
