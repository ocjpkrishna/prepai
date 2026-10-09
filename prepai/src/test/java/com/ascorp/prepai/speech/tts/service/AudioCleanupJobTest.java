package com.ascorp.prepai.speech.tts.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AudioCleanupJobTest {

	@Mock
	private AudioCleanupService cleanup;

	@Test
	void theNightlyRunHandsOverToTheCleanup() {
		new AudioCleanupJob(cleanup).removeStaleAudio();

		verify(cleanup).cleanUp();
	}
}
