package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.account.privacy.service.AccountGateService;
import com.ascorp.prepai.generation.imageextract.model.ExtractedProblem;
import com.ascorp.prepai.generation.imageextract.service.ImageExtractionService;
import com.ascorp.prepai.lesson.lesson.model.dto.ExtractResponse;
import com.ascorp.prepai.quota.ratelimit.service.ExtractLimiter;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Reads a photo so the student can confirm the text. It never uses up a session (spec 4.2). */
@Service
@RequiredArgsConstructor
public class LessonExtractService {

	private final AccountGateService accountGate;
	private final ExtractLimiter extractLimiter;
	private final ImageExtractionService extraction;

	public ExtractResponse extract(UUID userId, byte[] photo) {
		accountGate.requireLessonAccess(userId);
		extractLimiter.assertWithinExtractLimit(userId);
		ExtractedProblem problem = extraction.extract(photo);
		return new ExtractResponse(problem.problemText(), problem.confidence(), problem.hasDiagram());
	}
}
