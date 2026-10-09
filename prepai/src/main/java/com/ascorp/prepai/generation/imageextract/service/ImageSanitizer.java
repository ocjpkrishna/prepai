package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.generation.imageextract.model.ImageFormat;
import com.ascorp.prepai.generation.imageextract.model.ImageProperties;
import com.ascorp.prepai.generation.imageextract.model.SanitizedImage;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/** Checks the size and the magic bytes of an upload, then strips its metadata (spec 3.1.1, 2.7). */
@Component
public class ImageSanitizer {

	private final ImageProperties properties;
	private final Map<ImageFormat, MetadataStripper> strippers;

	public ImageSanitizer(ImageProperties properties, List<MetadataStripper> strippers) {
		this.properties = properties;
		this.strippers = strippers.stream()
				.collect(Collectors.toMap(MetadataStripper::format, Function.identity()));
	}

	public SanitizedImage sanitize(byte[] upload) {
		checkSize(upload);
		ImageFormat format = ImageFormat.detect(upload)
				.orElseThrow(() -> new ApiException(ErrorCode.IMAGE_UNSUPPORTED));
		return new SanitizedImage(format, strippers.get(format).strip(upload));
	}

	private void checkSize(byte[] upload) {
		if (upload.length > properties.maxBytes()) {
			throw new ApiException(ErrorCode.IMAGE_TOO_LARGE);
		}
	}
}
