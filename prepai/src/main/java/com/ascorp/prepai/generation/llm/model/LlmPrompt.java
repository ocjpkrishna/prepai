package com.ascorp.prepai.generation.llm.model;

/**
 * What one model call sends: the stable system prompt, the per-request user message (spec 6.1, 6.2) and, for image
 * extraction only, the photo (spec 3.1.1). A prompt with a photo is an image-extraction call.
 */
public record LlmPrompt(String system, String user, LlmImage image) {

	public LlmPrompt(String system, String user) {
		this(system, user, null);
	}

	public LlmPurpose purpose() {
		return image == null ? LlmPurpose.LESSON : LlmPurpose.IMAGE_EXTRACT;
	}
}
