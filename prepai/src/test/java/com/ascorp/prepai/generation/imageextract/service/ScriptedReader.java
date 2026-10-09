package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.llm.service.LlmProvider;
import java.util.ArrayList;
import java.util.List;

/** A provider that answers one photo prompt with a fixed text, or throws a fixed failure. Records the prompts. */
final class ScriptedReader implements LlmProvider {

	private final Object reply;
	private final List<LlmPrompt> prompts = new ArrayList<>();

	ScriptedReader(Object reply) {
		this.reply = reply;
	}

	@Override
	public String name() {
		return "scripted";
	}

	@Override
	public LlmCompletion complete(LlmPrompt prompt) {
		prompts.add(prompt);
		if (reply instanceof RuntimeException failure) {
			throw failure;
		}
		return new LlmCompletion(name(), "test-model", (String) reply, 100, 200, 0.5);
	}

	List<LlmPrompt> prompts() {
		return prompts;
	}
}
