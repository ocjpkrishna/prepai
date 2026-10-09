package com.ascorp.prepai.generation.quality.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.quality.model.QualityProperties;
import com.ascorp.prepai.generation.quality.model.VerifierGrade;
import com.ascorp.prepai.generation.quality.model.entity.QualityCorrection;
import com.ascorp.prepai.generation.quality.repository.QualityCorrectionRepository;
import com.ascorp.prepai.generation.rag.model.StoredProblem;
import com.ascorp.prepai.generation.rag.service.ProblemReviewService;
import com.ascorp.prepai.generation.validation.ValidLessons;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CorrectionServiceTest {

	private static final UUID ID = UUID.randomUUID();

	private final QualityCorrectionRepository corrections = mock(QualityCorrectionRepository.class);
	private final ProblemReviewService reviews = mock(ProblemReviewService.class);
	private final CorrectionService service = new CorrectionService(corrections, reviews,
			new QualityProperties(50, "claude-fable-5-1"));

	@Test
	void logsTheWrongAnswerAndItsCorrectionThenStoresTheCorrectionAsVerified() {
		LessonResponse lesson = ValidLessons.valid();
		LessonResponse corrected = ValidLessons.valid();
		StoredProblem problem = new StoredProblem(ID, "PHYSICS", "JEE_MAIN", "A block slides", lesson);

		service.apply(problem, new VerifierGrade(false, 4, 4, "SIGN"), corrected);

		ArgumentCaptor<QualityCorrection> captor = ArgumentCaptor.forClass(QualityCorrection.class);
		verify(corrections).save(captor.capture());
		QualityCorrection entry = captor.getValue();
		assertThat(entry.getProblemText()).isEqualTo("A block slides");
		assertThat(entry.getErrorType()).isEqualTo("SIGN");
		assertThat(entry.getVerifierModel()).isEqualTo("claude-fable-5-1");
		assertThat(entry.getOriginalAnswer()).contains("\"stepNumber\"");
		assertThat(entry.getCorrectedAnswer()).contains("\"stepNumber\"");
		verify(reviews).replaceSolution(ID, corrected);
	}

	@Test
	void saveIsCalledOnceEvenWhenTheGradeHasNoErrorType() {
		StoredProblem problem = new StoredProblem(ID, "PHYSICS", "JEE_MAIN", "A block slides", ValidLessons.valid());

		service.apply(problem, new VerifierGrade(true, 2, 4, null), ValidLessons.valid());

		verify(corrections).save(any(QualityCorrection.class));
	}
}
