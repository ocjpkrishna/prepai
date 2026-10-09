package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * One provider call behind the circuit breaker (spec 2.6). While the breaker is open the call is not made and the
 * student gets LLM_UNAVAILABLE at once. Validation happens later, so a bad answer never opens the breaker.
 */
@Service
@RequiredArgsConstructor
public class LlmCaller {

	public static final String BREAKER_NAME = "claude";
	private static final String OUTCOME_SUCCESS = "SUCCESS";
	private static final String OUTCOME_BREAKER_OPEN = "BREAKER_OPEN";

	private final LlmProvider provider;
	private final CircuitBreakerRegistry breakers;
	private final LlmMetrics metrics;

	public LlmCompletion call(LlmPrompt prompt) {
		CircuitBreaker breaker = breakers.circuitBreaker(BREAKER_NAME);
		try {
			LlmCompletion completion = breaker.executeSupplier(() -> provider.complete(prompt));
			metrics.recordCall(provider.name(), prompt.purpose(), OUTCOME_SUCCESS);
			metrics.recordUsage(completion, prompt.purpose());
			return completion;
		} catch (LlmProviderException e) {
			metrics.recordCall(provider.name(), prompt.purpose(), e.getReason().name());
			throw e;
		} catch (CallNotPermittedException e) {
			metrics.recordCall(provider.name(), prompt.purpose(), OUTCOME_BREAKER_OPEN);
			throw new ApiException(ErrorCode.LLM_UNAVAILABLE);
		}
	}
}
