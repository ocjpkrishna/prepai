package com.ascorp.prepai.speech.tts.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the audio cleanup every night at 03:00, after the account purge (02:15) and the verifier (02:30). */
@Component
@RequiredArgsConstructor
public class AudioCleanupJob {

	private static final String NIGHTLY_AT_0300 = "0 0 3 * * *";

	private final AudioCleanupService cleanup;

	@Scheduled(cron = NIGHTLY_AT_0300)
	public void removeStaleAudio() {
		cleanup.cleanUp();
	}
}
