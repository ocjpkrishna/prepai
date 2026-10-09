package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.lesson.lesson.model.LessonSource;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The lesson counter of spec 8.4 (`prepai_lesson_generated_total`); its cache hit rate comes from `source`. */
@Component
@RequiredArgsConstructor
public class LessonMetrics {

	private static final String GENERATED = "prepai.lesson.generated";

	private final MeterRegistry registry;

	public void recordGenerated(LessonSource source, LessonRequest request) {
		registry.counter(GENERATED, "source", source.name(), "subject", request.subject().name(), "exam",
				request.exam().name()).increment();
	}
}
