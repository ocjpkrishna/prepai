package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.llm.model.GenerationMetadata;
import com.ascorp.prepai.generation.llm.model.GenerationRun;
import com.ascorp.prepai.generation.llm.model.LessonGenerationResult;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.validation.model.ValidationResult;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The single entry point of `generation` for `lesson` (spec 2.6). It returns a lesson only after every validation
 * layer has passed, and otherwise the error the student sees. Nothing is stored here.
 */
@Service
@RequiredArgsConstructor
public class LessonGenerationService {

	private final GenerationRetryService retries;
	private final Clock clock;

	public LessonGenerationResult generate(LessonRequest request) {
		Instant started = clock.instant();
		GenerationRun run = runOrUnavailable(request);
		return new LessonGenerationResult(validLesson(run.validation()), metadata(run, started));
	}

	private GenerationRun runOrUnavailable(LessonRequest request) {
		try {
			return retries.run(request);
		} catch (LlmProviderException failure) {
			throw new ApiException(ErrorCode.LLM_UNAVAILABLE);
		}
	}

	private static LessonResponse validLesson(ValidationResult validation) {
		if (validation.outOfScope()) {
			throw new ApiException(ErrorCode.PROBLEM_OUT_OF_SCOPE);
		}
		if (!validation.isValid()) {
			throw new ApiException(ErrorCode.LESSON_GENERATION_FAILED);
		}
		return validation.lesson();
	}

	private GenerationMetadata metadata(GenerationRun run, Instant started) {
		return new GenerationMetadata(run.completion().provider(), run.completion().model(), run.retried(),
				run.retryReason(), run.attempts(), Duration.between(started, clock.instant()).toMillis());
	}
}
