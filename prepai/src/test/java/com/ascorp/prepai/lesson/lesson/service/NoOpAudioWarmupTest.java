package com.ascorp.prepai.lesson.lesson.service;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class NoOpAudioWarmupTest {

	@Test
	void warmingUpALessonDoesNothingAndNeverThrows() {
		assertThatCode(() -> new NoOpAudioWarmup().warmUp(LessonFixtures.lesson())).doesNotThrowAnyException();
	}
}
