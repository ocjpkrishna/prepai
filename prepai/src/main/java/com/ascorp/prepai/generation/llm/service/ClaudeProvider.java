package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Claude Sonnet 5.5 through Spring AI (spec 2.6, 8.4). Only the production profile selects it. Retries and timeouts
 * live in the app, not in the SDK (`max-retries: 0`), so failures are translated into {@link LlmProviderException}s
 * for the retry logic.
 */
@Component
@ConditionalOnProperty(prefix = "prepai.llm", name = "provider", havingValue = "claude", matchIfMissing = true)
public class ClaudeProvider implements LlmProvider {

	private static final String NAME = "claude";

	private final ChatModel chatModel;

	public ClaudeProvider(ChatModel chatModel) {
		this.chatModel = chatModel;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public LlmCompletion complete(LlmPrompt prompt) {
		try {
			return ClaudeTranslator.toCompletion(NAME, chatModel.call(ClaudeTranslator.toPrompt(prompt)));
		} catch (RestClientResponseException e) {
			throw ClaudeFailures.status(e);
		} catch (ResourceAccessException e) {
			throw ClaudeFailures.io(e);
		}
	}
}
