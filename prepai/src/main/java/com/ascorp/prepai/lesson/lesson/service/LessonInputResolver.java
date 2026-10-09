package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.generation.imageextract.service.ImageExtractionService;
import com.ascorp.prepai.quota.ratelimit.service.ExtractLimiter;
import java.util.Base64;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Makes sure a request carries text: an `IMAGE` request is read first and continues as a `PROBLEM` (spec 3.1.1). */
@Service
@RequiredArgsConstructor
public class LessonInputResolver {

	private final ExtractLimiter extractLimiter;
	private final ImageExtractionService extraction;

	public LessonRequest resolve(UUID userId, LessonRequest request) {
		if (request.type() == LessonRequest.Type.IMAGE) {
			return withText(request, LessonRequest.Type.PROBLEM, readPhoto(userId, request));
		}
		requireText(request.input().text());
		return request;
	}

	private String readPhoto(UUID userId, LessonRequest request) {
		String encoded = request.input().imageBase64();
		if (encoded == null || encoded.isBlank()) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED, "Please attach a photo.");
		}
		byte[] photo = decode(encoded);
		extractLimiter.assertWithinExtractLimit(userId);
		return extraction.extract(photo).problemText();
	}

	private static byte[] decode(String encoded) {
		try {
			return Base64.getMimeDecoder().decode(encoded);
		} catch (IllegalArgumentException notBase64) {
			throw new ApiException(ErrorCode.IMAGE_UNSUPPORTED);
		}
	}

	private static void requireText(String text) {
		if (text == null || text.isBlank()) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED, "Please type your question or topic.");
		}
	}

	private static LessonRequest withText(LessonRequest request, LessonRequest.Type type, String text) {
		return new LessonRequest(type, request.subject(), request.exam(), new LessonRequest.Input(text, null),
				request.difficulty(), request.language());
	}
}
