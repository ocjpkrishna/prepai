package com.ascorp.prepai.speech.tts.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.speech.tts.config.AudioProperties;
import com.ascorp.prepai.speech.tts.config.VoiceStudioProperties;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TtsWarmupServiceTest {

	private static final String LANGUAGE = "en-IN";
	private static final int WARMUP_CONCURRENCY = 2;
	private static final Duration SLOW = Duration.ofMillis(20);
	private static final List<String> NARRATIONS = List.of(
			"Step one: draw the free body diagram.",
			"Step two: resolve the forces.",
			"Step three: apply Newton's second law.",
			"Step four: solve for the acceleration.",
			"Summary: force equals mass times acceleration.");

	@TempDir
	private Path dir;

	@Test
	void everyNarrationOfTheLessonIsSynthesized() {
		FakeVoiceStudioClient voiceStudio = new FakeVoiceStudioClient();

		warmUp(voiceStudio).warmUp(NARRATIONS, LANGUAGE);

		assertThat(voiceStudio.synthesizeCalls()).isEqualTo(NARRATIONS.size());
	}

	@Test
	void aStoppedVoiceStudioNeverFailsTheWarmUp() {
		FakeVoiceStudioClient voiceStudio = new FakeVoiceStudioClient();
		voiceStudio.stop();

		warmUp(voiceStudio).warmUp(NARRATIONS, LANGUAGE);

		assertThat(voiceStudio.synthesizeCalls()).isEqualTo(NARRATIONS.size());
	}

	@Test
	void theWarmUpNeverRunsMoreCallsAtOnceThanTheConfiguredConcurrency() {
		FakeVoiceStudioClient voiceStudio = new FakeVoiceStudioClient(SLOW);

		warmUp(voiceStudio).warmUp(NARRATIONS, LANGUAGE);

		assertThat(voiceStudio.peakConcurrency()).isBetween(1, WARMUP_CONCURRENCY);
	}

	private TtsWarmupService warmUp(FakeVoiceStudioClient voiceStudio) {
		Clock clock = Clock.fixed(Instant.parse("2026-10-09T10:00:00Z"), ZoneOffset.UTC);
		AudioProperties audio = new AudioProperties(dir, 30, 10, WARMUP_CONCURRENCY);
		TtsService tts = new TtsService(voiceStudio, new AudioCacheService(audio, clock),
				new VoiceStudioProperties("unused", "en-IN-default"));
		return new TtsWarmupService(tts, audio);
	}
}
