package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.generation.imageextract.model.ImageFormat;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.springframework.stereotype.Component;

/**
 * Drops the APP1 (EXIF, XMP), APP3 to APP13 (IPTC, Photoshop) and APP15 segments and the comments. The colour profile
 * (APP2), the colour flags (APP14) and the picture data stay as they are.
 */
@Component
final class JpegMetadataStripper implements MetadataStripper {

	private static final int MARKER_PREFIX = 0xFF;
	private static final int MARKER_LENGTH = 2;
	private static final int SEGMENT_LENGTH_BYTES = 2;
	private static final int START_OF_IMAGE_LENGTH = 2;
	private static final int TEMPORARY_MARKER = 0x01;
	private static final int STANDALONE_FIRST = 0xD0;
	private static final int STANDALONE_LAST = 0xD9;
	private static final int START_OF_SCAN = 0xDA;
	private static final int APP1 = 0xE1;
	private static final int APP2 = 0xE2;
	private static final int APP14 = 0xEE;
	private static final int APP15 = 0xEF;
	private static final int COMMENT = 0xFE;

	@Override
	public ImageFormat format() {
		return ImageFormat.JPEG;
	}

	@Override
	public byte[] strip(byte[] bytes) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
		out.write(bytes, 0, START_OF_IMAGE_LENGTH);
		int offset = START_OF_IMAGE_LENGTH;
		while (offset < bytes.length) {
			int marker = marker(bytes, offset);
			if (marker == START_OF_SCAN) {
				out.write(bytes, offset, bytes.length - offset);
				return out.toByteArray();
			}
			int end = segmentEnd(bytes, offset, marker);
			if (!isMetadata(marker)) {
				out.write(bytes, offset, end - offset);
			}
			offset = end;
		}
		throw ImageBytes.malformed();
	}

	private static int marker(byte[] bytes, int offset) {
		if (offset + MARKER_LENGTH > bytes.length || Byte.toUnsignedInt(bytes[offset]) != MARKER_PREFIX) {
			throw ImageBytes.malformed();
		}
		return Byte.toUnsignedInt(bytes[offset + 1]);
	}

	private static int segmentEnd(byte[] bytes, int offset, int marker) {
		if (isStandalone(marker)) {
			return offset + MARKER_LENGTH;
		}
		int lengthField = offset + MARKER_LENGTH;
		if (lengthField + SEGMENT_LENGTH_BYTES > bytes.length) {
			throw ImageBytes.malformed();
		}
		int end = lengthField + segmentLength(bytes, lengthField);
		if (end > bytes.length) {
			throw ImageBytes.malformed();
		}
		return end;
	}

	private static int segmentLength(byte[] bytes, int lengthField) {
		return Short.toUnsignedInt(ByteBuffer.wrap(bytes, lengthField, SEGMENT_LENGTH_BYTES).getShort());
	}

	private static boolean isStandalone(int marker) {
		return marker == TEMPORARY_MARKER || (marker >= STANDALONE_FIRST && marker <= STANDALONE_LAST);
	}

	private static boolean isMetadata(int marker) {
		boolean appMetadata = marker >= APP1 && marker <= APP15 && marker != APP2 && marker != APP14;
		return appMetadata || marker == COMMENT;
	}
}
