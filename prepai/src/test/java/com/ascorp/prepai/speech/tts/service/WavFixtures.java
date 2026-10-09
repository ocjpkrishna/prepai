package com.ascorp.prepai.speech.tts.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Builds small WAV files: 16 kHz, mono, 16-bit, which is 32 bytes per millisecond. */
final class WavFixtures {

	static final int DEFAULT_MILLIS = 2000;
	private static final int HEADER_BYTES = 44;
	private static final int BYTES_PER_MILLI = 32;
	private static final int FMT_BODY_BYTES = 16;
	private static final int SAMPLE_RATE = 16000;
	private static final int BYTE_RATE = 32000;

	private WavFixtures() {
	}

	static byte[] wav(int durationMillis) {
		int dataBytes = durationMillis * BYTES_PER_MILLI;
		ByteBuffer buffer = ByteBuffer.allocate(HEADER_BYTES + dataBytes).order(ByteOrder.LITTLE_ENDIAN);
		buffer.put(ascii("RIFF")).putInt(HEADER_BYTES - 8 + dataBytes).put(ascii("WAVE"));
		buffer.put(ascii("fmt ")).putInt(FMT_BODY_BYTES);
		buffer.putShort((short) 1).putShort((short) 1).putInt(SAMPLE_RATE).putInt(BYTE_RATE);
		buffer.putShort((short) 2).putShort((short) 16);
		buffer.put(ascii("data")).putInt(dataBytes);
		return buffer.array();
	}

	static byte[] ascii(String text) {
		return text.getBytes(StandardCharsets.US_ASCII);
	}
}
