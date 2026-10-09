package com.ascorp.prepai.generation.llm.service;

import static com.ascorp.prepai.generation.validation.ValidLessons.validJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.generation.llm.model.GenerationRun;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class GenerationRetryServiceTest {

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private final CircuitBreakerRegistry breakers = LlmTestSupport.breakers();

	@Test
	void acceptsAValidFirstAnswerWithOneCall() {
		ScriptedProvider provider = new ScriptedProvider(validJson());

		GenerationRun run = service(provider, 2).run(LlmTestSupport.problemRequest());

		assertThat(run.validation().isValid()).isTrue();
		assertThat(run.attempts()).isEqualTo(1);
		assertThat(run.retried()).isFalse();
		assertThat(provider.prompts()).hasSize(1);
	}

	@Test
	void repairsAnInvalidAnswerWithThePromptThatListsTheErrors() {
		ScriptedProvider provider = new ScriptedProvider("not json at all", validJson());

		GenerationRun run = service(provider, 2).run(LlmTestSupport.problemRequest());

		assertThat(run.validation().isValid()).isTrue();
		assertThat(run.retryReason()).isEqualTo(RetryReason.VALIDATION_FAILED);
		assertThat(provider.prompts().get(1).user()).contains("failed these checks");
		assertThat(provider.prompts().get(1).user()).contains("not JSON that matches the lesson contract");
		assertThat(registry.counter("prepai.llm.retry", "reason", "VALIDATION_FAILED").count()).isEqualTo(1);
	}

	@Test
	void givesUpAfterTheLastAttemptWithTheInvalidRun() {
		ScriptedProvider provider = new ScriptedProvider("garbage", "still garbage");

		GenerationRun run = service(provider, 2).run(LlmTestSupport.problemRequest());

		assertThat(run.validation().isValid()).isFalse();
		assertThat(run.attempts()).isEqualTo(2);
		assertThat(provider.prompts()).hasSize(2);
	}

	@Test
	void retriesATransientFailureWithThePlainPrompt() {
		ScriptedProvider provider = new ScriptedProvider(new LlmProviderException(RetryReason.TIMEOUT,
				new SocketTimeoutException("read timed out")), validJson());

		GenerationRun run = service(provider, 2).run(LlmTestSupport.problemRequest());

		assertThat(run.validation().isValid()).isTrue();
		assertThat(run.retryReason()).isEqualTo(RetryReason.TIMEOUT);
		assertThat(provider.prompts().get(1).user()).doesNotContain("failed these checks");
	}

	@Test
	void rethrowsTheLastProviderFailureWhenEveryAttemptFails() {
		ScriptedProvider provider = new ScriptedProvider(
				new LlmProviderException(RetryReason.RATE_LIMITED, new IllegalStateException()),
				new LlmProviderException(RetryReason.PROVIDER_ERROR, new IllegalStateException()));

		assertThatThrownBy(() -> service(provider, 2).run(LlmTestSupport.problemRequest()))
				.isInstanceOfSatisfying(LlmProviderException.class,
						e -> assertThat(e.getReason()).isEqualTo(RetryReason.PROVIDER_ERROR));
		assertThat(provider.prompts()).hasSize(2);
		assertThat(registry.counter("prepai.llm.retry", "reason", "RATE_LIMITED").count()).isEqualTo(1);
	}

	@Test
	void doesNotRetryAnOutOfScopeAnswer() {
		ScriptedProvider provider = new ScriptedProvider("{\"error\": \"OUT_OF_SCOPE\"}");

		GenerationRun run = service(provider, 2).run(LlmTestSupport.problemRequest());

		assertThat(run.validation().outOfScope()).isTrue();
		assertThat(provider.prompts()).hasSize(1);
	}

	@Test
	void neverCallsTheProviderWhileTheBreakerIsOpen() {
		ScriptedProvider provider = new ScriptedProvider(validJson());
		breakers.circuitBreaker(LlmCaller.BREAKER_NAME).transitionToOpenState();

		assertThatThrownBy(() -> service(provider, 2).run(LlmTestSupport.problemRequest()))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE));
		assertThat(provider.prompts()).isEmpty();
	}

	private GenerationRetryService service(ScriptedProvider provider, int maxAttempts) {
		return LlmTestSupport.retryService(provider, registry, breakers, maxAttempts);
	}
}
