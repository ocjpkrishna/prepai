package com.ascorp.prepai.speech.tts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.speech.tts.config.AudioProperties;
import com.ascorp.prepai.speech.tts.config.VoiceStudioProperties;
import com.ascorp.prepai.speech.tts.model.TtsLimits;
import com.ascorp.prepai.speech.tts.model.dto.SynthesizeResponse;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TtsServiceTest {

	private static final String TEXT = "Newton's second law says force equals mass times acceleration.";
	private static final String LANGUAGE = "en-IN";
	private static final String VOICE = "en-IN-default";
	private static final Duration SLOW = Duration.ofMillis(100);
	private static final int SAME_NARRATION_CALLERS = 5;

	@TempDir
	private Path dir;

	private FakeVoiceStudioClient voiceStudio;
	private AudioCacheService cache;
	private TtsService tts;

	@BeforeEach
	void setUp() {
		voiceStudio = new FakeVoiceStudioClient(SLOW);
		Clock clock = Clock.fixed(Instant.parse("2026-10-09T10:00:00Z"), ZoneOffset.UTC);
		AudioProperties audio = new AudioProperties(dir, 30, 10, 2);
		cache = new AudioCacheService(audio, clock);
		tts = new TtsService(voiceStudio, cache, new VoiceStudioProperties("unused", VOICE));
	}

	@Test
	void aNewNarrationIsSynthesizedStoredAndReturnedAsAUrl() {
		SynthesizeResponse response = tts.synthesize(TEXT, LANGUAGE);

		assertThat(response.audioUrl()).startsWith("/audio/").endsWith(".wav");
		assertThat(response.durationMs()).isEqualTo(WavFixtures.DEFAULT_MILLIS);
		assertThat(cache.entries()).hasSize(1);
	}

	@Test
	void aRepeatedNarrationIsServedFromTheCacheWithoutAnotherCall() {
		SynthesizeResponse first = tts.synthesize(TEXT, LANGUAGE);
		SynthesizeResponse second = tts.synthesize(TEXT, LANGUAGE);

		assertThat(second).isEqualTo(first);
		assertThat(voiceStudio.synthesizeCalls()).isEqualTo(1);
	}

	@Test
	void simultaneousRequestsForOneNarrationCallVoiceStudioOnce() {
		ExecutorService callers = Executors.newFixedThreadPool(SAME_NARRATION_CALLERS);
		List<Future<SynthesizeResponse>> pending = IntStream.range(0, SAME_NARRATION_CALLERS)
				.mapToObj(i -> callers.submit(() -> tts.synthesize(TEXT, LANGUAGE)))
				.toList();
		List<SynthesizeResponse> answers = pending.stream().map(TtsServiceTest::join).toList();
		callers.shutdown();

		assertThat(voiceStudio.synthesizeCalls()).isEqualTo(1);
		assertThat(answers).allMatch(answer -> answer.equals(answers.getFirst()));
	}

	@Test
	void theSameTextInAnotherLanguageIsSynthesizedSeparately() {
		tts.synthesize(TEXT, LANGUAGE);
		tts.synthesize(TEXT, "hi-IN");

		assertThat(voiceStudio.synthesizeCalls()).isEqualTo(2);
	}

	@Test
	void textLongerThanTheLimitIsRejectedBeforeAnyCall() {
		String tooLong = "x".repeat(TtsLimits.MAX_TEXT_LENGTH + 1);

		assertThatThrownBy(() -> tts.synthesize(tooLong, LANGUAGE))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
		assertThat(voiceStudio.synthesizeCalls()).isZero();
	}

	@Test
	void aStoppedVoiceStudioIsTtsUnavailableAndNothingIsCached() {
		voiceStudio.stop();

		assertThatThrownBy(() -> tts.synthesize(TEXT, LANGUAGE))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getCode()).isEqualTo(ErrorCode.TTS_UNAVAILABLE));
		assertThat(cache.entries()).isEmpty();
	}

	@Test
	void theVoicesComeFromVoiceStudio() {
		assertThat(tts.voices()).isEqualTo(FakeVoiceStudioClient.VOICES);
	}

	private static SynthesizeResponse join(Future<SynthesizeResponse> future) {
		try {
			return future.get();
		} catch (InterruptedException | ExecutionException e) {
			throw new IllegalStateException("A caller failed in the test", e);
		}
	}
}
