package com.ascorp.prepai.lesson.lesson.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.lesson.lesson.model.LessonSource;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

class LessonMetricsTest {

	@Test
	void countsEachLessonBySourceSubjectAndExam() {
		SimpleMeterRegistry registry = new SimpleMeterRegistry();

		new LessonMetrics(registry).recordGenerated(LessonSource.REDIS_CACHE, LessonFixtures.problem());

		assertThat(registry.get("prepai.lesson.generated")
				.tags("source", "REDIS_CACHE", "subject", "PHYSICS", "exam", "JEE_MAIN").counter().count())
				.isEqualTo(1.0);
	}
}
