package com.ascorp.prepai.generation.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.generation.llm.model.ClaudeCodeProperties;
import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmImage;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class ClaudeCodeProviderTest {

	private static final LlmPrompt PROMPT = new LlmPrompt("system", "user");

	@Mock
	private ClaudeCodeProcess process;

	private ClaudeCodeProvider provider;

	@BeforeEach
	void setUp() {
		ClaudeCodeProperties settings = new ClaudeCodeProperties("claude", "sonnet", Duration.ofSeconds(5), 1);
		provider = new ClaudeCodeProvider(process, settings, JsonMapper.builder().build());
	}

	@Test
	void theResultTextAndTokenCountsBecomeTheCompletion() {
		when(process.run("system", "user")).thenReturn(
				"{\"is_error\":false,\"result\":\"{\\\"a\\\":1}\",\"usage\":{\"input_tokens\":7,\"output_tokens\":9}}");

		LlmCompletion completion = provider.complete(PROMPT);

		assertThat(completion.provider()).isEqualTo("claude-code");
		assertThat(completion.model()).isEqualTo("sonnet");
		assertThat(completion.text()).isEqualTo("{\"a\":1}");
		assertThat(completion.inputTokens()).isEqualTo(7);
		assertThat(completion.outputTokens()).isEqualTo(9);
	}

	@Test
	void aCodeFenceAroundTheJsonIsRemoved() {
		when(process.run("system", "user")).thenReturn(
				"{\"result\":\"```json\\n{\\\"a\\\":1}\\n```\"}");

		assertThat(provider.complete(PROMPT).text()).isEqualTo("{\"a\":1}");
	}

	@Test
	void anErrorReplyIsAProviderError() {
		when(process.run("system", "user")).thenReturn("{\"is_error\":true,\"result\":\"rate limited\"}");

		assertThatThrownBy(() -> provider.complete(PROMPT)).isInstanceOf(LlmProviderException.class);
	}

	@Test
	void anEmptyResultIsAProviderError() {
		when(process.run("system", "user")).thenReturn("{\"result\":\"   \"}");

		assertThatThrownBy(() -> provider.complete(PROMPT)).isInstanceOf(LlmProviderException.class);
	}

	@Test
	void textThatIsNotJsonIsAProviderError() {
		when(process.run("system", "user")).thenReturn("not json at all");

		assertThatThrownBy(() -> provider.complete(PROMPT)).isInstanceOf(LlmProviderException.class);
	}

	@Test
	void aPhotoIsRefusedBecauseClaudeCodeGetsTextOnly() {
		LlmPrompt withPhoto = new LlmPrompt("system", "user", new LlmImage("image/png", new byte[] {1}));

		assertThatThrownBy(() -> provider.complete(withPhoto)).isInstanceOf(LlmProviderException.class);
	}
}
