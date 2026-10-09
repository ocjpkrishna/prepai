package com.ascorp.prepai.generation.imageextract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.generation.imageextract.model.ImageFormat;
import com.ascorp.prepai.generation.imageextract.model.ImageProperties;
import com.ascorp.prepai.generation.imageextract.model.SanitizedImage;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImageSanitizerTest {

	private static final long MAX_BYTES = 10_000;
	private static final int IEND_CHUNK_LENGTH = 12;

	private final ImageSanitizer sanitizer = new ImageSanitizer(new ImageProperties(MAX_BYTES),
			List.of(new JpegMetadataStripper(), new PngMetadataStripper(), new WebpMetadataStripper()));

	@Test
	void removesTheExifAndCommentsFromAJpegAndKeepsThePicture() {
		SanitizedImage image = sanitizer.sanitize(ImageFixtures.jpegWithMetadata());

		assertThat(image.format()).isEqualTo(ImageFormat.JPEG);
		assertThat(image.bytes()).isEqualTo(ImageFixtures.jpegWithoutMetadata());
	}

	@Test
	void removesTheTextChunksFromAPngAndKeepsThePicture() {
		SanitizedImage image = sanitizer.sanitize(ImageFixtures.pngWithMetadata());

		assertThat(image.format()).isEqualTo(ImageFormat.PNG);
		assertThat(image.bytes()).isEqualTo(ImageFixtures.pngWithoutMetadata());
	}

	@Test
	void removesTheExifAndXmpChunksFromAWebpAndFixesTheRiffSize() {
		SanitizedImage image = sanitizer.sanitize(ImageFixtures.webpWithMetadata());

		assertThat(image.format()).isEqualTo(ImageFormat.WEBP);
		assertThat(image.bytes()).isEqualTo(ImageFixtures.webpWithoutMetadata());
	}

	@Test
	void refusesAFileOverTheSizeLimit() {
		byte[] tooBig = Arrays.copyOf(ImageFixtures.jpegWithMetadata(), (int) MAX_BYTES + 1);

		assertCode(tooBig, ErrorCode.IMAGE_TOO_LARGE);
	}

	@Test
	void refusesAFileWithoutAPhotoSignature() {
		assertCode("GIF89a........".getBytes(StandardCharsets.US_ASCII), ErrorCode.IMAGE_UNSUPPORTED);
	}

	@Test
	void refusesARiffFileThatIsNotWebp() {
		byte[] wave = ImageFixtures.webpWithoutMetadata();
		wave[8] = 'W';
		wave[9] = 'A';

		assertCode(wave, ErrorCode.IMAGE_UNSUPPORTED);
	}

	@Test
	void refusesAJpegCutOffInsideItsHeader() {
		byte[] truncated = Arrays.copyOf(ImageFixtures.jpegWithMetadata(), 5);

		assertCode(truncated, ErrorCode.IMAGE_UNSUPPORTED);
	}

	@Test
	void refusesAPngWithoutItsEndChunk() {
		byte[] png = ImageFixtures.pngWithoutMetadata();
		byte[] withoutEnd = Arrays.copyOf(png, png.length - IEND_CHUNK_LENGTH);

		assertCode(withoutEnd, ErrorCode.IMAGE_UNSUPPORTED);
	}

	private void assertCode(byte[] upload, ErrorCode code) {
		assertThatThrownBy(() -> sanitizer.sanitize(upload))
				.isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
	}
}
