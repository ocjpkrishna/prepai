package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.llm.prompt.ProblemTags;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import com.ascorp.prepai.generation.quality.model.VerifierGrade;
import java.util.List;
import org.springframework.ai.anthropic.AnthropicCacheOptions;
import org.springframework.ai.anthropic.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The verifier's prompt and reply (spec 8.1, steps 3 and 4), kept apart from the client so that each class stays small.
 * The system prompt is stable, so it is marked for Anthropic's prompt cache.
 */
final class VerifierExchange {

	private static final JsonMapper JSON = JsonMapper.builder().build();
	private static final String PROBLEM_CLOSE = "</problem>";

	private VerifierExchange() {
	}

	static Prompt prompt(String model, String problemText, LessonResponse lesson) {
		String problem = ProblemTags.strip(problemText);
		String user = "<problem>\n" + problem + "\n" + PROBLEM_CLOSE
				+ "\n\nLESSON:\n" + JSON.writeValueAsString(lesson);
		AnthropicChatOptions options = AnthropicChatOptions.builder()
				.model(model)
				.cacheOptions(AnthropicCacheOptions.builder().strategy(AnthropicCacheStrategy.SYSTEM_ONLY).build())
				.build();
		return new Prompt(List.of(new SystemMessage(VerifierSystemPrompt.text()), new UserMessage(user)), options);
	}

	/** A reply that is not the grade JSON counts as a failed call: the row stays unverified and is tried again. */
	static VerifierGrade grade(ChatResponse response) {
		String text = response.getResult().getOutput().getText();
		try {
			return JSON.readValue(text, VerifierGrade.class);
		} catch (JacksonException e) {
			throw new LlmProviderException(RetryReason.PROVIDER_ERROR, e);
		}
	}
}
