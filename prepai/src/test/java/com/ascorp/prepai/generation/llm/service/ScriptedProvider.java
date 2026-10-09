package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/** A provider that answers from a script: each reply is a text, or a RuntimeException to throw. Records the prompts. */
final class ScriptedProvider implements LlmProvider {

	static final String NAME = "scripted";
	static final String MODEL = "test-model";

	private final Deque<Object> script;
	private final List<LlmPrompt> prompts = new ArrayList<>();

	ScriptedProvider(Object... replies) {
		this.script = new ArrayDeque<>(List.of(replies));
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public LlmCompletion complete(LlmPrompt prompt) {
		prompts.add(prompt);
		Object next = script.poll();
		if (next instanceof RuntimeException failure) {
			throw failure;
		}
		return new LlmCompletion(NAME, MODEL, (String) next, 100, 200, 0.5);
	}

	List<LlmPrompt> prompts() {
		return prompts;
	}
}
