package com.ascorp.prepai.generation.imageextract.service;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/** Tiny JPEG, PNG and WebP files with and without metadata. Picture data is a placeholder; structure is real. */
final class ImageFixtures {

	private static final int APP0 = 0xE0;
	private static final int EXIF_APP1 = 0xE1;
	private static final int COMMENT = 0xFE;
	private static final int SOF0 = 0xC0;
	private static final int SOS = 0xDA;

	private ImageFixtures() {
	}

	static byte[] jpegWithMetadata() {
		return concat(bytes(0xFF, 0xD8), segment(APP0, 0x4A, 0x46),
				segment(EXIF_APP1, 'E', 'x', 'i', 'f', 0, 0), segment(COMMENT, 'h', 'i'),
				segment(SOF0, 0x08), scanAndEnd());
	}

	static byte[] jpegWithoutMetadata() {
		return concat(bytes(0xFF, 0xD8), segment(APP0, 0x4A, 0x46), segment(SOF0, 0x08), scanAndEnd());
	}

	static byte[] pngWithMetadata() {
		return concat(pngSignature(), chunk("IHDR", new int[13]), chunk("eXIf", 'G', 'P', 'S'),
				chunk("tEXt", 'a'), chunk("IDAT", 1, 2), chunk("IEND"));
	}

	static byte[] pngWithoutMetadata() {
		return concat(pngSignature(), chunk("IHDR", new int[13]), chunk("IDAT", 1, 2), chunk("IEND"));
	}

	static byte[] webpWithMetadata() {
		return riff(webpChunk("VP8 ", 1, 2, 3), webpChunk("EXIF", 1, 2, 3, 4), webpChunk("XMP ", 5, 6));
	}

	static byte[] webpWithoutMetadata() {
		return riff(webpChunk("VP8 ", 1, 2, 3));
	}

	private static byte[] scanAndEnd() {
		return concat(segment(SOS, 0x01), bytes(1, 2, 3, 0xFF, 0xD9));
	}

	private static byte[] pngSignature() {
		return bytes(0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A);
	}

	private static byte[] segment(int marker, int... payload) {
		int length = payload.length + 2;
		return concat(bytes(0xFF, marker, length >> 8, length & 0xFF), bytes(payload));
	}

	private static byte[] chunk(String type, int... data) {
		byte[] length = ByteBuffer.allocate(Integer.BYTES).putInt(data.length).array();
		return concat(length, ascii(type), bytes(data), new byte[Integer.BYTES]);
	}

	private static byte[] webpChunk(String tag, int... data) {
		byte[] size = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(data.length).array();
		return concat(ascii(tag), size, bytes(data), new byte[data.length % 2]);
	}

	private static byte[] riff(byte[]... chunks) {
		int body = ascii("WEBP").length;
		for (byte[] chunk : chunks) {
			body += chunk.length;
		}
		byte[] size = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(body).array();
		return concat(ascii("RIFF"), size, ascii("WEBP"), concat(chunks));
	}

	private static byte[] concat(byte[]... parts) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		for (byte[] part : parts) {
			out.writeBytes(part);
		}
		return out.toByteArray();
	}

	private static byte[] bytes(int... values) {
		byte[] bytes = new byte[values.length];
		for (int i = 0; i < values.length; i++) {
			bytes[i] = (byte) values[i];
		}
		return bytes;
	}

	private static byte[] ascii(String text) {
		return text.getBytes(StandardCharsets.US_ASCII);
	}
}
