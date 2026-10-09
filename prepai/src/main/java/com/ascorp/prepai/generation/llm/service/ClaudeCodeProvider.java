package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.generation.llm.model.ClaudeCodeProperties;
import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * The lesson model reached through Claude Code, so it runs on the operator's Claude plan and needs no API key
 * (`prepai.llm.provider=claude-code`). Photos are not supported: Claude Code is given text only.
 */
@Component
@ConditionalOnProperty(prefix = "prepai.llm", name = "provider", havingValue = "claude-code")
@RequiredArgsConstructor
public class ClaudeCodeProvider implements LlmProvider {

	private static final String NAME = "claude-code";
	private static final String FENCE = "```";

	private final ClaudeCodeProcess process;
	private final ClaudeCodeProperties settings;
	private final JsonMapper json;

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public LlmCompletion complete(LlmPrompt prompt) {
		if (prompt.image() != null) {
			throw new LlmProviderException(RetryReason.PROVIDER_ERROR,
					new UnsupportedOperationException("Claude Code cannot read photos"));
		}
		return toCompletion(readReply(process.run(prompt.system(), prompt.user())));
	}

	private JsonNode readReply(String raw) {
		try {
			JsonNode reply = json.readTree(raw);
			if (reply.path("is_error").asBoolean(false) || reply.path("result").asString("").isBlank()) {
				throw new LlmProviderException(RetryReason.PROVIDER_ERROR,
						new IllegalStateException("Claude Code returned no answer"));
			}
			return reply;
		} catch (JacksonException unreadable) {
			throw new LlmProviderException(RetryReason.PROVIDER_ERROR, unreadable);
		}
	}

	private LlmCompletion toCompletion(JsonNode reply) {
		JsonNode usage = reply.path("usage");
		return new LlmCompletion(NAME, settings.model(), withoutFence(reply.path("result").asString()),
				usage.path("input_tokens").asInt(0), usage.path("output_tokens").asInt(0), 0.0);
	}

	/** The model sometimes wraps its JSON in a code fence; the lesson parser wants the bare JSON. */
	private static String withoutFence(String text) {
		String trimmed = text.strip();
		if (!trimmed.startsWith(FENCE)) {
			return trimmed;
		}
		int firstLineEnd = trimmed.indexOf('\n');
		int closing = trimmed.lastIndexOf(FENCE);
		boolean wellFormed = firstLineEnd >= 0 && closing > firstLineEnd;
		return wellFormed ? trimmed.substring(firstLineEnd + 1, closing).strip() : trimmed;
	}
}
