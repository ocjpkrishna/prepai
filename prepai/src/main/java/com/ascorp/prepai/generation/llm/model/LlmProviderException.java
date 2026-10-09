package com.ascorp.prepai.generation.llm.model;

import lombok.Getter;

/**
 * A provider call that failed in a way a retry may fix: a timeout, a rate limit, or a server error. Validation
 * failures are not provider failures and never use this class.
 */
@Getter
public class LlmProviderException extends RuntimeException {

	private final RetryReason reason;

	public LlmProviderException(RetryReason reason, Throwable cause) {
		super(reason.name(), cause);
		this.reason = reason;
	}
}
