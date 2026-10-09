package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.generation.llm.model.GenerationRun;
import com.ascorp.prepai.generation.llm.model.LlmCompletion;
import com.ascorp.prepai.generation.llm.model.LlmPrompt;
import com.ascorp.prepai.generation.llm.model.LlmProperties;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import com.ascorp.prepai.generation.llm.prompt.PromptTemplateService;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import com.ascorp.prepai.generation.validation.model.ValidationResult;
import com.ascorp.prepai.generation.validation.service.LessonValidator;
import java.util.List;
import java.util.concurrent.locks.LockSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The attempts of spec 2.6. A transient provider failure is retried with the plain prompt after a short pause. A
 * response that fails validation is retried with a repair prompt that lists the errors. Both count against
 * `prepai.llm.max-attempts`, so a request makes at most two model calls.
 */
@Service
@RequiredArgsConstructor
public class GenerationRetryService {

	private final LlmCaller caller;
	private final PromptTemplateService prompts;
	private final LessonValidator validator;
	private final LlmMetrics metrics;
	private final LlmProperties properties;

	public GenerationRun run(LessonRequest request) {
		return attempt(request, 1, null, List.of());
	}

	private GenerationRun attempt(LessonRequest request, int number, RetryReason reason,
			List<ValidationError> errors) {
		LlmPrompt prompt = errors.isEmpty() ? prompts.firstAttempt(request) : prompts.repair(request, errors);
		LlmCompletion completion;
		try {
			completion = caller.call(prompt);
		} catch (LlmProviderException failure) {
			return retryProviderFailure(request, number, failure);
		}
		return judge(request, number, reason, completion);
	}

	private GenerationRun judge(LessonRequest request, int number, RetryReason reason, LlmCompletion completion) {
		ValidationResult validation = validator.validate(completion.text());
		if (validation.isValid() || validation.outOfScope()) {
			return new GenerationRun(validation, completion, number, reason);
		}
		metrics.recordValidationFailure(completion.provider(), validation.errors());
		if (number >= properties.maxAttempts()) {
			return new GenerationRun(validation, completion, number, reason);
		}
		return retry(request, number, RetryReason.VALIDATION_FAILED, validation.errors());
	}

	private GenerationRun retryProviderFailure(LessonRequest request, int number, LlmProviderException failure) {
		if (number >= properties.maxAttempts()) {
			throw failure;
		}
		LockSupport.parkNanos(properties.retryBackoff().toNanos());
		return retry(request, number, failure.getReason(), List.of());
	}

	private GenerationRun retry(LessonRequest request, int number, RetryReason reason, List<ValidationError> errors) {
		metrics.recordRetry(reason);
		return attempt(request, number + 1, reason, errors);
	}
}
