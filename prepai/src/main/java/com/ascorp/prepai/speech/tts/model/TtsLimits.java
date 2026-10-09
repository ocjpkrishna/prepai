package com.ascorp.prepai.speech.tts.model;

/** Hard limits of the TTS API (spec 4.5). Constants, so they can also appear in annotations. */
public final class TtsLimits {

	/** Longest text one request may synthesize, in characters. */
	public static final int MAX_TEXT_LENGTH = 1000;

	private TtsLimits() {
	}
}
