package com.ascorp.prepai.generation.llm.model;

/** One photo sent with a prompt (spec 3.1.1). It is held in memory only and never stored. */
public record LlmImage(String mimeType, byte[] bytes) {
}
