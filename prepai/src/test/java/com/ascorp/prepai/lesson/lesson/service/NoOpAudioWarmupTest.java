package com.ascorp.prepai.lesson.lesson.service;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class NoOpAudioWarmupTest {

	@Test
	void warmingUpAStoredLessonDoesNothingAndNeverFails() {
		assertThatCode(() -> new NoOpAudioWarmup().warmUp(LessonFixtures.lesson())).doesNotThrowAnyException();
	}
}
