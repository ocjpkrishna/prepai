package com.ascorp.prepai.generation.llm.model;

/** The text a provider returned for one call, with the token counts and the cost it reported. */
public record LlmCompletion(String provider, String model, String text, int inputTokens, int outputTokens,
		double costUsd) {
}
