package com.ascorp.prepai.generation.quality.service;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.quality.model.QualityProperties;
import com.ascorp.prepai.generation.quality.model.VerifierGrade;
import com.ascorp.prepai.generation.quality.model.entity.QualityCorrection;
import com.ascorp.prepai.generation.quality.repository.QualityCorrectionRepository;
import com.ascorp.prepai.generation.rag.model.StoredProblem;
import com.ascorp.prepai.generation.rag.service.ProblemReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * Records a wrong lesson and its correction (spec 8.1, step 5a and 5d). The Redis exact-match key is not evicted: it
 * hashes the student's original text, which the stored row does not keep, so the 7-day TTL bounds how long it lasts.
 */
@Service
@Profile("prod")
@RequiredArgsConstructor
public class CorrectionService {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final QualityCorrectionRepository corrections;
	private final ProblemReviewService reviews;
	private final QualityProperties properties;

	@Transactional
	public void apply(StoredProblem problem, VerifierGrade grade, LessonResponse corrected) {
		corrections.save(entry(problem, grade, corrected));
		reviews.replaceSolution(problem.id(), corrected);
	}

	private QualityCorrection entry(StoredProblem problem, VerifierGrade grade, LessonResponse corrected) {
		return QualityCorrection.builder()
				.problemText(problem.problemText())
				.originalAnswer(JSON.writeValueAsString(problem.lesson()))
				.correctedAnswer(JSON.writeValueAsString(corrected))
				.errorType(grade.errorType())
				.verifierModel(properties.verifierModel())
				.build();
	}
}
