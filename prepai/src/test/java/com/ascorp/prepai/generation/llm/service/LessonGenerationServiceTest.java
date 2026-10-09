package com.ascorp.prepai.generation.llm.service;

import static com.ascorp.prepai.generation.validation.ValidLessons.validJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.generation.llm.model.LessonGenerationResult;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;

class LessonGenerationServiceTest {

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();

	@Test
	void returnsTheValidLessonWithItsMetadata() {
		LessonGenerationResult result = service(new ScriptedProvider(validJson())).generate(
				LlmTestSupport.problemRequest());

		assertThat(result.lesson().title()).isEqualTo("Projectile Motion");
		assertThat(result.metadata().provider()).isEqualTo(ScriptedProvider.NAME);
		assertThat(result.metadata().model()).isEqualTo(ScriptedProvider.MODEL);
		assertThat(result.metadata().retried()).isFalse();
		assertThat(result.metadata().validationAttempts()).isEqualTo(1);
		assertThat(result.metadata().generationMs()).isZero();
	}

	@Test
	void reportsAnOutOfScopeProblemAs422() {
		assertThatThrownBy(() -> service(new ScriptedProvider("{\"error\": \"OUT_OF_SCOPE\"}"))
				.generate(LlmTestSupport.problemRequest()))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getCode()).isEqualTo(ErrorCode.PROBLEM_OUT_OF_SCOPE));
	}

	@Test
	void reportsLessonGenerationFailedWhenBothAnswersAreInvalid() {
		assertThatThrownBy(() -> service(new ScriptedProvider("garbage", "garbage"))
				.generate(LlmTestSupport.problemRequest()))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getCode()).isEqualTo(ErrorCode.LESSON_GENERATION_FAILED));
	}

	@Test
	void reportsLlmUnavailableWhenTheProviderFailsOnEveryAttempt() {
		ScriptedProvider provider = new ScriptedProvider(
				new LlmProviderException(RetryReason.TIMEOUT, new IllegalStateException()),
				new LlmProviderException(RetryReason.TIMEOUT, new IllegalStateException()));

		assertThatThrownBy(() -> service(provider).generate(LlmTestSupport.problemRequest()))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE));
	}

	private LessonGenerationService service(ScriptedProvider provider) {
		Clock clock = Clock.fixed(Instant.parse("2026-10-09T10:00:00Z"), ZoneOffset.UTC);
		GenerationRetryService retries = LlmTestSupport.retryService(provider, registry,
				LlmTestSupport.breakers(), 2);
		return new LessonGenerationService(retries, clock);
	}
}
