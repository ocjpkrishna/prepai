package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.generation.imageextract.model.ImageFormat;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Drops the EXIF and XMP chunks of a WebP (RIFF) file and rewrites the RIFF size to match what is left. The image and
 * colour-profile chunks stay as they are.
 */
@Component
final class WebpMetadataStripper implements MetadataStripper {

	private static final int RIFF_HEADER_LENGTH = 12;
	private static final int CHUNK_HEADER_LENGTH = 8;
	private static final int TAG_LENGTH = 4;
	private static final int RIFF_SIZE_OFFSET = 4;
	private static final int RIFF_SIZE_EXCLUDES = 8;
	private static final Set<String> METADATA_CHUNKS = Set.of("EXIF", "XMP ");

	@Override
	public ImageFormat format() {
		return ImageFormat.WEBP;
	}

	@Override
	public byte[] strip(byte[] bytes) {
		if (bytes.length < RIFF_HEADER_LENGTH + CHUNK_HEADER_LENGTH) {
			throw ImageBytes.malformed();
		}
		ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
		out.write(bytes, 0, RIFF_HEADER_LENGTH);
		int offset = RIFF_HEADER_LENGTH;
		while (offset < bytes.length) {
			long end = chunkEnd(bytes, offset);
			if (!METADATA_CHUNKS.contains(ImageBytes.ascii(bytes, offset, TAG_LENGTH))) {
				out.write(bytes, offset, (int) end - offset);
			}
			offset = (int) end;
		}
		return withRiffSize(out.toByteArray());
	}

	private static long chunkEnd(byte[] bytes, int offset) {
		if (offset + CHUNK_HEADER_LENGTH > bytes.length) {
			throw ImageBytes.malformed();
		}
		long size = Integer.toUnsignedLong(ByteBuffer.wrap(bytes, offset + TAG_LENGTH, Integer.BYTES)
				.order(ByteOrder.LITTLE_ENDIAN).getInt());
		long padded = size + size % 2;
		long end = offset + CHUNK_HEADER_LENGTH + padded;
		if (end > bytes.length) {
			throw ImageBytes.malformed();
		}
		return end;
	}

	private static byte[] withRiffSize(byte[] riff) {
		int riffSize = riff.length - RIFF_SIZE_EXCLUDES;
		ByteBuffer.wrap(riff).order(ByteOrder.LITTLE_ENDIAN).putInt(RIFF_SIZE_OFFSET, riffSize);
		return riff;
	}
}
