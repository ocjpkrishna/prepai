package com.ascorp.prepai.speech.tts.model;

/** One cached narration: its content hash and how long it plays. The file is served by nginx at {@link #url()}. */
public record AudioFile(String hash, long durationMs) {

	private static final String URL_PREFIX = "/audio/";
	private static final String EXTENSION = ".wav";

	public String fileName() {
		return hash + EXTENSION;
	}

	public String url() {
		return URL_PREFIX + fileName();
	}
}
