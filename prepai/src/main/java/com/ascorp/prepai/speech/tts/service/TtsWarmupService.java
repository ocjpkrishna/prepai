package com.ascorp.prepai.speech.tts.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.speech.tts.config.AudioProperties;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * Synthesizes a stored lesson's narration in the background, so playback finds the audio cached (spec 4.5). It runs
 * after the lesson is returned, never blocks it, and never fails it: a failed narration is only logged.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TtsWarmupService {

	private final TtsService tts;
	private final AudioProperties audio;

	@Async
	public void warmUp(List<String> narrations, String language) {
		Semaphore permits = new Semaphore(audio.warmupConcurrency());
		try (ExecutorService workers = Executors.newVirtualThreadPerTaskExecutor()) {
			narrations.forEach(text -> workers.submit(() -> warmOne(text, language, permits)));
		}
	}

	private void warmOne(String text, String language, Semaphore permits) {
		permits.acquireUninterruptibly();
		try {
			tts.synthesize(text, language);
		} catch (ApiException e) {
			log.warn("Narration warm-up skipped: {}", e.getCode());
		} finally {
			permits.release();
		}
	}
}
