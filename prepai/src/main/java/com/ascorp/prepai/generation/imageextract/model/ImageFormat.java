package com.ascorp.prepai.generation.imageextract.model;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;

/** The photo formats a student may send (spec 3.1.1), told apart by their magic bytes, never by Content-Type. */
public enum ImageFormat {
	JPEG("image/jpeg"),
	PNG("image/png"),
	WEBP("image/webp");

	private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
	private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', (byte) 0x1A, '\n'};
	private static final byte[] RIFF_MAGIC = "RIFF".getBytes(StandardCharsets.US_ASCII);
	private static final byte[] WEBP_MAGIC = "WEBP".getBytes(StandardCharsets.US_ASCII);
	private static final int WEBP_TAG_OFFSET = 8;

	private final String mimeType;

	ImageFormat(String mimeType) {
		this.mimeType = mimeType;
	}

	public String mimeType() {
		return mimeType;
	}

	public static Optional<ImageFormat> detect(byte[] bytes) {
		if (startsWith(bytes, 0, JPEG_MAGIC)) {
			return Optional.of(JPEG);
		}
		if (startsWith(bytes, 0, PNG_MAGIC)) {
			return Optional.of(PNG);
		}
		boolean webp = startsWith(bytes, 0, RIFF_MAGIC) && startsWith(bytes, WEBP_TAG_OFFSET, WEBP_MAGIC);
		return webp ? Optional.of(WEBP) : Optional.empty();
	}

	private static boolean startsWith(byte[] bytes, int offset, byte[] magic) {
		int end = offset + magic.length;
		return bytes.length >= end && Arrays.equals(bytes, offset, end, magic, 0, magic.length);
	}
}
