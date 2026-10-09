package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.generation.imageextract.model.ImageFormat;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.util.Set;
import org.springframework.stereotype.Component;

/** Drops the eXIf and text chunks (tEXt, zTXt, iTXt) and stops at IEND, so trailing bytes are not kept either. */
@Component
final class PngMetadataStripper implements MetadataStripper {

	private static final int SIGNATURE_LENGTH = 8;
	private static final int CHUNK_OVERHEAD = 12;
	private static final int TYPE_OFFSET = 4;
	private static final int TYPE_LENGTH = 4;
	private static final String END_CHUNK = "IEND";
	private static final Set<String> METADATA_CHUNKS = Set.of("eXIf", "tEXt", "zTXt", "iTXt");

	@Override
	public ImageFormat format() {
		return ImageFormat.PNG;
	}

	@Override
	public byte[] strip(byte[] bytes) {
		ByteArrayOutputStream out = new ByteArrayOutputStream(bytes.length);
		out.write(bytes, 0, SIGNATURE_LENGTH);
		int offset = SIGNATURE_LENGTH;
		while (offset + CHUNK_OVERHEAD <= bytes.length) {
			int end = chunkEnd(bytes, offset);
			String type = ImageBytes.ascii(bytes, offset + TYPE_OFFSET, TYPE_LENGTH);
			if (!METADATA_CHUNKS.contains(type)) {
				out.write(bytes, offset, end - offset);
			}
			if (END_CHUNK.equals(type)) {
				return out.toByteArray();
			}
			offset = end;
		}
		throw ImageBytes.malformed();
	}

	private static int chunkEnd(byte[] bytes, int offset) {
		long length = Integer.toUnsignedLong(ByteBuffer.wrap(bytes, offset, Integer.BYTES).getInt());
		long end = offset + CHUNK_OVERHEAD + length;
		if (end > bytes.length) {
			throw ImageBytes.malformed();
		}
		return (int) end;
	}
}
