package com.ascorp.prepai.generation.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import com.ascorp.prepai.generation.quality.model.QualityProperties;
import com.ascorp.prepai.generation.quality.model.VerifierGrade;
import com.ascorp.prepai.generation.validation.ValidLessons;
import java.net.SocketTimeoutException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.anthropic.AnthropicChatOptions;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;

class ClaudeVerifierClientTest {

	private static final String GRADE_JSON = "{\"correct\": true, \"stepQuality\": 4, \"teachingClarity\": 5, "
			+ "\"errorType\": null}";

	private final ChatModel chatModel = mock(ChatModel.class);
	private final ClaudeVerifierClient client = new ClaudeVerifierClient(chatModel,
			new QualityProperties(50, "claude-fable-5-1"));

	@Test
	void parsesTheGradeReturnedByTheVerifier() {
		when(chatModel.call(any(Prompt.class))).thenReturn(response(GRADE_JSON));

		VerifierGrade grade = client.grade("A block slides", ValidLessons.valid());

		assertThat(grade).isEqualTo(new VerifierGrade(true, 4, 5, null));
	}

	@Test
	void callsTheVerifierModelNamedInConfiguration() {
		when(chatModel.call(any(Prompt.class))).thenReturn(response(GRADE_JSON));

		client.grade("A block slides", ValidLessons.valid());

		ArgumentCaptor<Prompt> captor = ArgumentCaptor.forClass(Prompt.class);
		verify(chatModel).call(captor.capture());
		assertThat(((AnthropicChatOptions) captor.getValue().getOptions()).getModel()).isEqualTo("claude-fable-5-1");
	}

	@Test
	void aReplyThatIsNotTheGradeJsonIsAProviderError() {
		when(chatModel.call(any(Prompt.class))).thenReturn(response("I think it is right"));

		assertThatThrownBy(() -> client.grade("A block slides", ValidLessons.valid()))
				.isInstanceOfSatisfying(LlmProviderException.class,
						failure -> assertThat(failure.getReason()).isEqualTo(RetryReason.PROVIDER_ERROR));
	}

	@Test
	void aTooManyRequestsReplyIsRateLimited() {
		when(chatModel.call(any(Prompt.class))).thenThrow(new HttpClientErrorException(HttpStatus.TOO_MANY_REQUESTS));

		assertReason(RetryReason.RATE_LIMITED);
	}

	@Test
	void aReadTimeoutIsATimeout() {
		when(chatModel.call(any(Prompt.class))).thenThrow(
				new ResourceAccessException("read timed out", new SocketTimeoutException()));

		assertReason(RetryReason.TIMEOUT);
	}

	private void assertReason(RetryReason reason) {
		assertThatThrownBy(() -> client.grade("A block slides", ValidLessons.valid()))
				.isInstanceOfSatisfying(LlmProviderException.class,
						failure -> assertThat(failure.getReason()).isEqualTo(reason));
	}

	private static ChatResponse response(String text) {
		return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
	}
}
