package com.ascorp.prepai.generation.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmImage;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.metadata.ChatResponseMetadata;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;

class ClaudeProviderTest {

	private static final LlmPrompt PROMPT = new LlmPrompt("system", "user");

	private final ChatModel chatModel = mock(ChatModel.class);
	private final ClaudeProvider provider = new ClaudeProvider(chatModel);

	@Test
	void mapsTheReplyUsageAndCostOfOneCall() {
		ChatResponse response = response("{\"title\": \"x\"}", 1_000_000, 100_000);
		when(chatModel.call(any(Prompt.class))).thenReturn(response);

		LlmCompletion completion = provider.complete(PROMPT);

		assertThat(completion.provider()).isEqualTo("claude");
		assertThat(completion.model()).isEqualTo("claude-sonnet-5-5");
		assertThat(completion.text()).isEqualTo("{\"title\": \"x\"}");
		assertThat(completion.inputTokens()).isEqualTo(1_000_000);
		assertThat(completion.outputTokens()).isEqualTo(100_000);
		assertThat(completion.costUsd()).isCloseTo(3.0, within(1e-9));
	}

	@Test
	void mapsATooManyRequestsReplyToRateLimited() {
		when(chatModel.call(any(Prompt.class))).thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

		assertReason(RetryReason.RATE_LIMITED);
	}

	@Test
	void mapsAServerErrorToProviderError() {
		when(chatModel.call(any(Prompt.class))).thenThrow(
				new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

		assertReason(RetryReason.PROVIDER_ERROR);
	}

	@Test
	void mapsAReadTimeoutToTimeout() {
		when(chatModel.call(any(Prompt.class))).thenThrow(
				new ResourceAccessException("read timed out", new SocketTimeoutException()));

		assertReason(RetryReason.TIMEOUT);
	}

	@Test
	void mapsAReplyWithNoAnswerToProviderError() {
		ChatResponse empty = mock(ChatResponse.class);
		when(empty.getResult()).thenReturn(null);
		when(chatModel.call(any(Prompt.class))).thenReturn(empty);

		assertReason(RetryReason.PROVIDER_ERROR);
	}

	@Test
	void mapsAConnectionFailureToProviderError() {
		when(chatModel.call(any(Prompt.class))).thenThrow(
				new ResourceAccessException("connection refused", new ConnectException()));

		assertReason(RetryReason.PROVIDER_ERROR);
	}

	@Test
	void sendsThePhotoAsMediaInTheUserMessage() {
		ChatResponse response = response("{}", 1, 1);
		when(chatModel.call(any(Prompt.class))).thenReturn(response);

		provider.complete(new LlmPrompt("system", "read", new LlmImage("image/png", new byte[] {1, 2})));

		ArgumentCaptor<Prompt> sent = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel).call(sent.capture());
		UserMessage user = (UserMessage) sent.getValue().getInstructions().get(1);
		assertThat(user.getMedia()).singleElement()
				.satisfies(media -> assertThat(media.getMimeType().toString()).isEqualTo("image/png"));
	}

	private void assertReason(RetryReason reason) {
		assertThatThrownBy(() -> provider.complete(PROMPT))
				.isInstanceOfSatisfying(LlmProviderException.class, e -> assertThat(e.getReason()).isEqualTo(reason));
	}

	private static ChatResponse response(String text, int inputTokens, int outputTokens) {
		Usage usage = mock(Usage.class);
		when(usage.getPromptTokens()).thenReturn(inputTokens);
		when(usage.getCompletionTokens()).thenReturn(outputTokens);
		ChatResponseMetadata metadata = mock(ChatResponseMetadata.class);
		when(metadata.getUsage()).thenReturn(usage);
		when(metadata.getModel()).thenReturn("claude-sonnet-5-5");
		Generation generation = mock(Generation.class);
		when(generation.getOutput()).thenReturn(new AssistantMessage(text));
		ChatResponse response = mock(ChatResponse.class);
		when(response.getResult()).thenReturn(generation);
		when(response.getMetadata()).thenReturn(metadata);
		return response;
	}
}
