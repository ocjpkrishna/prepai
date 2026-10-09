package com.ascorp.prepai.generation.llm.model;

import com.ascorp.prepai.generation.validation.model.ValidationResult;

/**
 * The end of the attempts: the last validation outcome, the completion it came from, how many calls were made and
 * why the last one was repeated (null when the first call was not repeated).
 */
public record GenerationRun(ValidationResult validation, LlmCompletion completion, int attempts,
		RetryReason retryReason) {

	public boolean retried() {
		return attempts > 1;
	}
}
