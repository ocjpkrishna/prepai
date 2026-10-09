package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.generation.imageextract.model.ExtractedProblem;
import com.ascorp.prepai.generation.imageextract.model.SanitizedImage;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.service.LlmCaller;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The single entry point of `generation` for photos (spec 3.1.1). The photo is checked and stripped, read once by the
 * vision model, and kept only in memory. It is never stored, and a LOW reading is refused.
 */
@Service
@RequiredArgsConstructor
public class ImageExtractionService {

	private final ImageSanitizer sanitizer;
	private final ExtractionReplyParser replies;
	private final LlmCaller llm;

	public ExtractedProblem extract(byte[] upload) {
		SanitizedImage image = sanitizer.sanitize(upload);
		ExtractedProblem problem = replies.parse(read(image));
		if (!problem.readable()) {
			throw new ApiException(ErrorCode.IMAGE_UNREADABLE);
		}
		return problem;
	}

	private String read(SanitizedImage image) {
		try {
			return llm.call(ExtractionPrompt.forPhoto(image)).text();
		} catch (LlmProviderException failure) {
			throw new ApiException(ErrorCode.LLM_UNAVAILABLE);
		}
	}
}
