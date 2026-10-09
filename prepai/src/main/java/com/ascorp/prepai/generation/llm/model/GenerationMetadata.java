package com.ascorp.prepai.generation.llm.model;

/** How a lesson was produced: the model that answered, whether it was repeated, and how long it took (spec 2.6). */
public record GenerationMetadata(String provider, String model, boolean retried, RetryReason retryReason,
		int validationAttempts, long generationMs) {
}
