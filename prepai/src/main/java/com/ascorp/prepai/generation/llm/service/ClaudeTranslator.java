package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmImage;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import java.util.List;
import org.springframework.ai.anthropic.AnthropicCacheOptions;
import org.springframework.ai.anthropic.AnthropicCacheStrategy;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;

/**
 * Converts between PrepAI's prompt and completion records and Spring AI's. The system prompt is marked for Anthropic's
 * prompt cache (spec 2.6), and the cost of a call is computed at Sonnet 5.5 list prices.
 */
final class ClaudeTranslator {

	private static final double INPUT_USD_PER_MILLION_TOKENS = 2.0;
	private static final double OUTPUT_USD_PER_MILLION_TOKENS = 10.0;
	private static final double TOKENS_PER_MILLION = 1_000_000.0;

	private ClaudeTranslator() {
	}

	static Prompt toPrompt(LlmPrompt prompt) {
		AnthropicChatOptions options = AnthropicChatOptions.builder()
				.cacheOptions(AnthropicCacheOptions.builder().strategy(AnthropicCacheStrategy.SYSTEM_ONLY).build())
				.build();
		return new Prompt(List.of(new SystemMessage(prompt.system()), userMessage(prompt)), options);
	}

	private static UserMessage userMessage(LlmPrompt prompt) {
		if (prompt.image() == null) {
			return new UserMessage(prompt.user());
		}
		LlmImage image = prompt.image();
		Media photo = new Media(MimeType.valueOf(image.mimeType()), new ByteArrayResource(image.bytes()));
		return UserMessage.builder().text(prompt.user()).media(photo).build();
	}

	static LlmCompletion toCompletion(String provider, ChatResponse response) {
		AssistantMessage message = answerOf(response);
		Usage usage = response.getMetadata().getUsage();
		int inputTokens = usage.getPromptTokens();
		int outputTokens = usage.getCompletionTokens();
		return new LlmCompletion(provider, response.getMetadata().getModel(), message.getText(), inputTokens,
				outputTokens, cost(inputTokens, outputTokens));
	}

	/** A reply without an answer (no generation, no text) is a provider failure, not a crash. */
	private static AssistantMessage answerOf(ChatResponse response) {
		if (response.getResult() == null || response.getResult().getOutput() == null) {
			throw new LlmProviderException(RetryReason.PROVIDER_ERROR,
					new IllegalStateException("The model returned no answer"));
		}
		return response.getResult().getOutput();
	}

	private static double cost(int inputTokens, int outputTokens) {
		return inputTokens * INPUT_USD_PER_MILLION_TOKENS / TOKENS_PER_MILLION
				+ outputTokens * OUTPUT_USD_PER_MILLION_TOKENS / TOKENS_PER_MILLION;
	}
}
