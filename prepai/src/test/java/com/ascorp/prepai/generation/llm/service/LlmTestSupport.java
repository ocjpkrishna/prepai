package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.common.model.enums.Difficulty;
import com.ascorp.prepai.common.model.enums.Exam;
import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.generation.llm.model.LlmProperties;
import com.ascorp.prepai.generation.llm.prompt.PromptTemplateService;
import com.ascorp.prepai.generation.validation.rule.CanvasBoundsRule;
import com.ascorp.prepai.generation.validation.rule.CanvasRule;
import com.ascorp.prepai.generation.validation.rule.ParseRule;
import com.ascorp.prepai.generation.validation.rule.StructureRule;
import com.ascorp.prepai.generation.validation.rule.TextSafetyRule;
import com.ascorp.prepai.generation.validation.service.LessonSanitizer;
import com.ascorp.prepai.generation.validation.service.LessonValidator;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Duration;
import java.util.List;
import org.springframework.core.io.ClassPathResource;

/** Builds the generation pipeline by hand, so each test controls the provider, the breaker and the attempts. */
public final class LlmTestSupport {

	private LlmTestSupport() {
	}

	static LessonRequest problemRequest() {
		return new LessonRequest(LessonRequest.Type.PROBLEM, Subject.PHYSICS, Exam.JEE_MAIN,
				new LessonRequest.Input("A ball is thrown at 20 m/s.", null), Difficulty.MEDIUM, Language.EN);
	}

	static LessonValidator validator() {
		return new LessonValidator(new ParseRule(), new LessonSanitizer(), List.of(new StructureRule(),
				new CanvasRule(), new CanvasBoundsRule(), new TextSafetyRule()));
	}

	public static PromptTemplateService prompts() {
		return new PromptTemplateService(new ClassPathResource("prompts/system-prompt.txt"),
				new ClassPathResource("prompts/user-prompt.txt"));
	}

	static LlmProperties properties(int maxAttempts) {
		return new LlmProperties("scripted", maxAttempts, Duration.ZERO);
	}

	static CircuitBreakerRegistry breakers() {
		return CircuitBreakerRegistry.ofDefaults();
	}

	static LlmMetrics metrics(SimpleMeterRegistry registry, CircuitBreakerRegistry breakers) {
		return new LlmMetrics(registry, breakers);
	}

	static GenerationRetryService retryService(ScriptedProvider provider, SimpleMeterRegistry registry,
			CircuitBreakerRegistry breakers, int maxAttempts) {
		LlmMetrics metrics = metrics(registry, breakers);
		LlmCaller caller = new LlmCaller(provider, breakers, metrics);
		return new GenerationRetryService(caller, prompts(), validator(), metrics, properties(maxAttempts));
	}
}
