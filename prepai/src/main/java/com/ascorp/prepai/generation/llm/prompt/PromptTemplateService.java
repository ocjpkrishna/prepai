package com.ascorp.prepai.generation.llm.prompt;

import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

/**
 * Builds the prompts of spec 6.1 and 6.2. The student's text is wrapped in {@code <problem>} tags and the tags inside
 * it are removed, so it cannot close the block early. A repair prompt adds the validator's errors (spec 2.6).
 */
@Service
public class PromptTemplateService {

	private static final String PROBLEM_OPEN = "<problem>";
	private static final String PROBLEM_CLOSE = "</problem>";
	private static final String REPAIR_NOTE = "\n\nYour previous answer failed these checks. "
			+ "Return the complete corrected JSON only, fixing every point:";

	private final String systemPrompt;
	private final String userTemplate;

	public PromptTemplateService(@Value("classpath:prompts/system-prompt.txt") Resource system,
			@Value("classpath:prompts/user-prompt.txt") Resource user) {
		this.systemPrompt = read(system);
		this.userTemplate = read(user);
	}

	/** The first call of a request. */
	public LlmPrompt firstAttempt(LessonRequest request) {
		return new LlmPrompt(systemPrompt, userPrompt(request));
	}

	/** The second call after a validation failure: the same request plus the reasons the first answer failed. */
	public LlmPrompt repair(LessonRequest request, List<ValidationError> errors) {
		return new LlmPrompt(systemPrompt, userPrompt(request) + repairNote(errors));
	}

	private String userPrompt(LessonRequest request) {
		return userTemplate
				.replace("{subject}", request.subject().name())
				.replace("{exam}", request.exam().name())
				.replace("{difficulty}", request.difficulty().name())
				.replace("{input_text}", problemText(request));
	}

	private static String problemText(LessonRequest request) {
		return Objects.toString(request.input().text(), "").replace(PROBLEM_OPEN, "").replace(PROBLEM_CLOSE, "");
	}

	private static String repairNote(List<ValidationError> errors) {
		StringBuilder note = new StringBuilder(REPAIR_NOTE);
		errors.forEach(error -> note.append("\n- ").append(error.message()));
		return note.toString();
	}

	private static String read(Resource resource) {
		try {
			return resource.getContentAsString(StandardCharsets.UTF_8);
		} catch (IOException e) {
			throw new UncheckedIOException("Prompt template is missing: " + resource.getDescription(), e);
		}
	}
}
