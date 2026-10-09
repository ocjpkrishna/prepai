package com.ascorp.prepai.generation.quality.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.service.LessonGenerationService;
import com.ascorp.prepai.generation.quality.model.VerificationReport;
import com.ascorp.prepai.generation.quality.model.VerificationReport.Outcome;
import com.ascorp.prepai.generation.quality.model.VerifierGrade;
import com.ascorp.prepai.generation.rag.model.StoredProblem;
import com.ascorp.prepai.generation.rag.service.ProblemReviewService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

/**
 * The nightly verifier (spec 8.1). A correct lesson is marked verified; a wrong one is corrected and the correction is
 * stored as verified. One failed row never stops the batch: it stays unverified and is tried again the next night.
 */
@Slf4j
@Service
@Profile("prod")
@RequiredArgsConstructor
public class VerificationService {

	private final ProblemReviewService reviews;
	private final VerifierClient verifier;
	private final LessonGenerationService generation;
	private final CorrectionService corrections;

	public VerificationReport verifyBatch(int batchSize) {
		VerificationReport report = VerificationReport.empty();
		for (StoredProblem problem : reviews.pending(batchSize)) {
			report = report.record(reviewSafely(problem));
		}
		return report;
	}

	private Outcome reviewSafely(StoredProblem problem) {
		try {
			return review(problem);
		} catch (LlmProviderException | ApiException failure) {
			log.warn("Verification of problem {} failed, it stays unverified: {}", problem.id(), failure.getMessage());
			return Outcome.FAILED;
		}
	}

	private Outcome review(StoredProblem problem) {
		VerifierGrade grade = verifier.grade(problem.problemText(), problem.lesson());
		if (grade.passes()) {
			reviews.markVerified(problem.id());
			return Outcome.VERIFIED;
		}
		corrections.apply(problem, grade, generation.generate(problem.toRequest()).lesson());
		return Outcome.CORRECTED;
	}
}
