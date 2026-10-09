package com.ascorp.prepai.speech.tts.service;

import com.ascorp.prepai.speech.tts.model.VoiceStudioRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * The name of a narration's audio: SHA-256 of (text, language, voice) (spec 4.5). The language and voice never
 * contain a line break, so the fields can always be told apart, even when the text itself has one.
 */
final class AudioKey {

	private static final String ALGORITHM = "SHA-256";
	private static final String SEPARATOR = "\n";

	private AudioKey() {
	}

	static String of(VoiceStudioRequest request) {
		String material = String.join(SEPARATOR, request.text(), request.language(), request.voice());
		return HexFormat.of().formatHex(sha256(material.getBytes(StandardCharsets.UTF_8)));
	}

	private static byte[] sha256(byte[] input) {
		try {
			return MessageDigest.getInstance(ALGORITHM).digest(input);
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalStateException("Every Java runtime provides SHA-256", e);
		}
	}
}
