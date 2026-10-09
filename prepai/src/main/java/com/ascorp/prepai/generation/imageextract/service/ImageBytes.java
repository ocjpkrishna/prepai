package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.nio.charset.StandardCharsets;

/** Small helpers shared by the metadata strippers. */
final class ImageBytes {

	private ImageBytes() {
	}

	/** A file whose structure does not match its magic bytes is refused as an unsupported photo (spec 4.7). */
	static ApiException malformed() {
		return new ApiException(ErrorCode.IMAGE_UNSUPPORTED);
	}

	static String ascii(byte[] bytes, int offset, int length) {
		return new String(bytes, offset, length, StandardCharsets.US_ASCII);
	}
}
