package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Development and test provider (decision 7): it returns one canned lesson, the spec 3.2 sample, whatever the
 * question, and one canned extraction for any photo. No key or network is needed, and the real validator still
 * checks the lesson.
 */
@Component
@ConditionalOnProperty(prefix = "prepai.llm", name = "provider", havingValue = "fake")
public class FakeLessonProvider implements LlmProvider {

	private static final String NAME = "fake";

	private final String cannedLesson;
	private final String cannedExtraction;

	public FakeLessonProvider(@Value("classpath:prompts/lesson-example.json") Resource lesson,
			@Value("classpath:llm/fake-extraction.json") Resource extraction) {
		this.cannedLesson = read(lesson);
		this.cannedExtraction = read(extraction);
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public LlmCompletion complete(LlmPrompt prompt) {
		String text = prompt.image() == null ? cannedLesson : cannedExtraction;
		return new LlmCompletion(NAME, NAME, text, 0, 0, 0.0);
	}

	private static String read(Resource resource) {
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Canned reply is missing: " + resource.getDescription(), e);
		}
	}
}
