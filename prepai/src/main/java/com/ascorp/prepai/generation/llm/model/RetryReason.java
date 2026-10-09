package com.ascorp.prepai.generation.llm.model;

/** Why a lesson generation attempt was repeated (spec 2.6 and 8.4). */
public enum RetryReason {
	PROVIDER_ERROR,
	TIMEOUT,
	RATE_LIMITED,
	VALIDATION_FAILED
}
