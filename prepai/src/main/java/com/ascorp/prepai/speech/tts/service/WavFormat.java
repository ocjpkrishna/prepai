package com.ascorp.prepai.speech.tts.service;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Reads the RIFF/WAVE header of narration audio: whether the bytes are WAV at all, and how long they play.
 * Duration is computed from the header, so the TTS server does not have to report it.
 */
final class WavFormat {

	private static final String RIFF = "RIFF";
	private static final String WAVE = "WAVE";
	private static final String FMT = "fmt ";
	private static final String DATA = "data";
	private static final int CHUNK_ID_BYTES = 4;
	private static final int RIFF_HEADER_BYTES = 12;
	private static final int CHUNK_HEADER_BYTES = 8;
	private static final int CHUNK_SIZE_OFFSET = 4;
	private static final int BYTE_RATE_OFFSET = 8;
	private static final int WAVE_OFFSET = 8;
	private static final int MILLIS_PER_SECOND = 1000;

	private WavFormat() {
	}

	static boolean isWav(byte[] bytes) {
		return bytes != null
				&& bytes.length >= RIFF_HEADER_BYTES
				&& RIFF.equals(ascii(bytes, 0))
				&& WAVE.equals(ascii(bytes, WAVE_OFFSET));
	}

	/** Duration in milliseconds, read from the chunk headers only. Returns 0 when no data chunk is found. */
	static long durationMillis(byte[] head) {
		ByteBuffer buffer = ByteBuffer.wrap(head).order(ByteOrder.LITTLE_ENDIAN);
		long byteRate = 0;
		long offset = RIFF_HEADER_BYTES;
		while (offset + CHUNK_HEADER_BYTES <= head.length) {
			int at = (int) offset;
			String id = ascii(head, at);
			long size = Integer.toUnsignedLong(buffer.getInt(at + CHUNK_SIZE_OFFSET));
			if (FMT.equals(id)) {
				byteRate = Integer.toUnsignedLong(buffer.getInt(at + CHUNK_HEADER_BYTES + BYTE_RATE_OFFSET));
			}
			if (DATA.equals(id)) {
				return byteRate == 0 ? 0 : size * MILLIS_PER_SECOND / byteRate;
			}
			offset += CHUNK_HEADER_BYTES + size + size % 2;
		}
		return 0;
	}

	private static String ascii(byte[] bytes, int offset) {
		return new String(bytes, offset, CHUNK_ID_BYTES, StandardCharsets.US_ASCII);
	}
}
