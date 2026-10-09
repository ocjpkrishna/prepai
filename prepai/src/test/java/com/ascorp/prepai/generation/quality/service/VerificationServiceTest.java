package com.ascorp.prepai.generation.quality.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.llm.model.LessonGenerationResult;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import com.ascorp.prepai.generation.llm.service.LessonGenerationService;
import com.ascorp.prepai.generation.quality.model.VerificationReport;
import com.ascorp.prepai.generation.quality.model.VerifierGrade;
import com.ascorp.prepai.generation.rag.model.StoredProblem;
import com.ascorp.prepai.generation.rag.service.ProblemReviewService;
import com.ascorp.prepai.generation.validation.ValidLessons;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class VerificationServiceTest {

	private static final int BATCH = 50;
	private static final UUID ID = UUID.randomUUID();
	private static final LessonResponse LESSON = ValidLessons.valid();
	private static final StoredProblem PROBLEM = new StoredProblem(ID, "PHYSICS", "JEE_MAIN", "A block slides", LESSON);
	private static final VerifierGrade CORRECT = new VerifierGrade(true, 4, 4, null);

	private final ProblemReviewService reviews = mock(ProblemReviewService.class);
	private final VerifierClient verifier = mock(VerifierClient.class);
	private final LessonGenerationService generation = mock(LessonGenerationService.class);
	private final CorrectionService corrections = mock(CorrectionService.class);
	private final VerificationService service = new VerificationService(reviews, verifier, generation, corrections);

	@Test
	void aCorrectLessonIsMarkedVerifiedWithoutACorrection() {
		when(reviews.pending(BATCH)).thenReturn(List.of(PROBLEM));
		when(verifier.grade(PROBLEM.problemText(), LESSON)).thenReturn(CORRECT);

		VerificationReport report = service.verifyBatch(BATCH);

		assertThat(report).isEqualTo(new VerificationReport(1, 0, 0));
		verify(reviews).markVerified(ID);
		verifyNoInteractions(generation, corrections);
	}

	@Test
	void aWrongAnswerIsCorrectedAndTheCorrectionIsStored() {
		VerifierGrade wrong = new VerifierGrade(false, 4, 4, "SIGN");
		LessonResponse corrected = ValidLessons.valid();
		when(reviews.pending(BATCH)).thenReturn(List.of(PROBLEM));
		when(verifier.grade(PROBLEM.problemText(), LESSON)).thenReturn(wrong);
		when(generation.generate(any(LessonRequest.class))).thenReturn(new LessonGenerationResult(corrected, null));

		VerificationReport report = service.verifyBatch(BATCH);

		assertThat(report).isEqualTo(new VerificationReport(0, 1, 0));
		verify(corrections).apply(PROBLEM, wrong, corrected);
		verify(reviews, never()).markVerified(any());
	}

	@Test
	void aLowStepQualityLessonIsCorrectedEvenWhenTheAnswerIsRight() {
		VerifierGrade shaky = new VerifierGrade(true, 2, 4, null);
		LessonResponse corrected = ValidLessons.valid();
		when(reviews.pending(BATCH)).thenReturn(List.of(PROBLEM));
		when(verifier.grade(PROBLEM.problemText(), LESSON)).thenReturn(shaky);
		when(generation.generate(any(LessonRequest.class))).thenReturn(new LessonGenerationResult(corrected, null));

		VerificationReport report = service.verifyBatch(BATCH);

		assertThat(report.corrected()).isEqualTo(1);
		verify(corrections).apply(PROBLEM, shaky, corrected);
	}

	@Test
	void aFailedVerifierCallLeavesTheRowUnverifiedAndTheBatchGoesOn() {
		StoredProblem second = new StoredProblem(UUID.randomUUID(), "PHYSICS", "JEE_MAIN", "A ball falls", LESSON);
		when(reviews.pending(BATCH)).thenReturn(List.of(PROBLEM, second));
		when(verifier.grade(PROBLEM.problemText(), LESSON))
				.thenThrow(new LlmProviderException(RetryReason.TIMEOUT, new IllegalStateException("slow")));
		when(verifier.grade(second.problemText(), LESSON)).thenReturn(CORRECT);

		VerificationReport report = service.verifyBatch(BATCH);

		assertThat(report).isEqualTo(new VerificationReport(1, 0, 1));
		verify(reviews, never()).markVerified(ID);
	}

	@Test
	void aCorrectionThatFailsValidationCountsAsFailed() {
		when(reviews.pending(BATCH)).thenReturn(List.of(PROBLEM));
		when(verifier.grade(PROBLEM.problemText(), LESSON)).thenReturn(new VerifierGrade(false, 1, 1, "CONCEPT"));
		when(generation.generate(any(LessonRequest.class)))
				.thenThrow(new ApiException(ErrorCode.LESSON_GENERATION_FAILED));

		VerificationReport report = service.verifyBatch(BATCH);

		assertThat(report).isEqualTo(new VerificationReport(0, 0, 1));
		verifyNoInteractions(corrections);
	}
}
