package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPurpose;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.List;
import org.springframework.stereotype.Component;

/** The LLM metrics of spec 8.4, exported to Prometheus with the `prepai_` prefix. */
@Component
public class LlmMetrics {

	private final MeterRegistry registry;

	public LlmMetrics(MeterRegistry registry, CircuitBreakerRegistry breakers) {
		this.registry = registry;
		CircuitBreaker breaker = breakers.circuitBreaker(LlmCaller.BREAKER_NAME);
		Gauge.builder("prepai.llm.circuit.breaker.state", breaker, LlmMetrics::openAsOne)
				.tag("provider", LlmCaller.BREAKER_NAME)
				.register(registry);
	}

	public void recordCall(String provider, LlmPurpose purpose, String outcome) {
		registry.counter("prepai.llm.calls", "provider", provider, "purpose", purpose.name(), "outcome", outcome)
				.increment();
	}

	public void recordUsage(LlmCompletion completion, LlmPurpose purpose) {
		String provider = completion.provider();
		registry.counter("prepai.llm.tokens", "provider", provider, "direction", "input")
				.increment(completion.inputTokens());
		registry.counter("prepai.llm.tokens", "provider", provider, "direction", "output")
				.increment(completion.outputTokens());
		registry.counter("prepai.llm.cost.usd", "provider", provider, "purpose", purpose.name())
				.increment(completion.costUsd());
	}

	public void recordRetry(RetryReason reason) {
		registry.counter("prepai.llm.retry", "reason", reason.name()).increment();
	}

	public void recordValidationFailure(String provider, List<ValidationError> errors) {
		errors.stream()
				.map(ValidationError::layer)
				.distinct()
				.forEach(layer -> registry.counter("prepai.llm.validation.failures", "provider", provider,
						"layer", layer.name()).increment());
	}

	private static double openAsOne(CircuitBreaker breaker) {
		return breaker.getState() == CircuitBreaker.State.OPEN ? 1 : 0;
	}
}
