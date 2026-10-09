package com.ascorp.prepai.speech.tts.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.speech.tts.model.VoiceStudioRequest;
import com.ascorp.prepai.speech.tts.model.dto.VoiceDto;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Stands in for VoiceStudio in tests (decision 6). It returns a 2-second WAV, counts its calls and tracks the most
 * calls running at once. Switched off, it behaves like a stopped server.
 */
final class FakeVoiceStudioClient implements VoiceStudioClient {

	static final List<VoiceDto> VOICES = List.of(new VoiceDto("en-IN-default", "English (India)", "en-IN"));

	private final AtomicInteger synthesizeCalls = new AtomicInteger();
	private final AtomicInteger running = new AtomicInteger();
	private final AtomicInteger peak = new AtomicInteger();
	private final Duration delay;
	private volatile boolean up = true;

	FakeVoiceStudioClient() {
		this(Duration.ZERO);
	}

	FakeVoiceStudioClient(Duration delay) {
		this.delay = delay;
	}

	@Override
	public byte[] synthesize(VoiceStudioRequest request) {
		synthesizeCalls.incrementAndGet();
		if (!up) {
			throw new ApiException(ErrorCode.TTS_UNAVAILABLE);
		}
		int now = running.incrementAndGet();
		peak.accumulateAndGet(now, Math::max);
		LockSupport.parkNanos(delay.toNanos());
		running.decrementAndGet();
		return WavFixtures.wav(WavFixtures.DEFAULT_MILLIS);
	}

	@Override
	public List<VoiceDto> voices() {
		if (!up) {
			throw new ApiException(ErrorCode.TTS_UNAVAILABLE);
		}
		return VOICES;
	}

	void stop() {
		up = false;
	}

	int synthesizeCalls() {
		return synthesizeCalls.get();
	}

	int peakConcurrency() {
		return peak.get();
	}
}
