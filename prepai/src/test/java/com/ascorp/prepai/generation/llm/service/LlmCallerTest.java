package com.ascorp.prepai.generation.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.SocketTimeoutException;
import org.junit.jupiter.api.Test;

class LlmCallerTest {

	private static final LlmPrompt PROMPT = new LlmPrompt("system", "user");

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private final CircuitBreakerRegistry breakers = LlmTestSupport.breakers();

	@Test
	void countsASuccessfulCallAndItsTokensAndCost() {
		LlmCaller caller = caller(new ScriptedProvider("{}"));

		LlmCompletion completion = caller.call(PROMPT);

		assertThat(completion.text()).isEqualTo("{}");
		assertThat(registry.counter("prepai.llm.calls", "provider", "scripted", "purpose", "LESSON",
				"outcome", "SUCCESS").count()).isEqualTo(1);
		assertThat(registry.counter("prepai.llm.tokens", "provider", "scripted", "direction", "output").count())
				.isEqualTo(200);
		assertThat(registry.counter("prepai.llm.cost.usd", "provider", "scripted", "purpose", "LESSON").count())
				.isEqualTo(0.5);
	}

	@Test
	void countsAFailedCallByItsReasonAndRethrowsIt() {
		LlmCaller caller = caller(new ScriptedProvider(
				new LlmProviderException(RetryReason.TIMEOUT, new SocketTimeoutException())));

		assertThatThrownBy(() -> caller.call(PROMPT)).isInstanceOf(LlmProviderException.class);
		assertThat(registry.counter("prepai.llm.calls", "provider", "scripted", "purpose", "LESSON",
				"outcome", "TIMEOUT").count()).isEqualTo(1);
	}

	@Test
	void failsFastWithLlmUnavailableWhenTheBreakerIsOpen() {
		breakers.circuitBreaker(LlmCaller.BREAKER_NAME).transitionToOpenState();
		LlmCaller caller = caller(new ScriptedProvider("{}"));

		assertThatThrownBy(() -> caller.call(PROMPT))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE));
		assertThat(registry.counter("prepai.llm.calls", "provider", "scripted", "purpose", "LESSON",
				"outcome", "BREAKER_OPEN").count()).isEqualTo(1);
	}

	@Test
	void reportsTheBreakerAsOpenInItsGauge() {
		breakers.circuitBreaker(LlmCaller.BREAKER_NAME).transitionToOpenState();

		LlmTestSupport.metrics(registry, breakers);

		assertThat(registry.get("prepai.llm.circuit.breaker.state").gauge().value()).isEqualTo(1);
	}

	private LlmCaller caller(ScriptedProvider provider) {
		return new LlmCaller(provider, breakers, LlmTestSupport.metrics(registry, breakers));
	}
}
