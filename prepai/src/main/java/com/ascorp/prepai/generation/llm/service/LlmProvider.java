package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;

/**
 * One model behind the pipeline. Exactly one provider is active (`prepai.llm.provider`). A failure a retry may fix is
 * reported as an {@link com.ascorp.prepai.generation.llm.model.LlmProviderException}.
 */
public interface LlmProvider {

	String name();

	LlmCompletion complete(LlmPrompt prompt);
}
