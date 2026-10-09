package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/** Turns Spring AI's HTTP and I/O failures into the retry reasons of spec 2.6 and 8.4, for every Claude call. */
final class ClaudeFailures {

	private ClaudeFailures() {
	}

	static LlmProviderException status(RestClientResponseException failure) {
		RetryReason reason = failure.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)
				? RetryReason.RATE_LIMITED
				: RetryReason.PROVIDER_ERROR;
		return new LlmProviderException(reason, failure);
	}

	static LlmProviderException io(ResourceAccessException failure) {
		Throwable cause = failure.getMostSpecificCause();
		boolean timedOut = cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException;
		return new LlmProviderException(timedOut ? RetryReason.TIMEOUT : RetryReason.PROVIDER_ERROR, failure);
	}
}
