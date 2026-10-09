package com.ascorp.prepai.lesson.lesson.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import org.junit.jupiter.api.Test;

class NoOpAudioWarmupTest {

	@Test
	void warmingUpAnyLessonIsSafeAndDoesNothing() {
		assertDoesNotThrow(() -> new NoOpAudioWarmup().warmUp(LessonFixtures.lesson()));
	}
}
