package com.ascorp.prepai.speech.tts.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.speech.tts.config.VoiceStudioProperties;
import com.ascorp.prepai.speech.tts.model.AudioFile;
import com.ascorp.prepai.speech.tts.model.TtsLimits;
import com.ascorp.prepai.speech.tts.model.VoiceStudioRequest;
import com.ascorp.prepai.speech.tts.model.dto.SynthesizeResponse;
import com.ascorp.prepai.speech.tts.model.dto.VoiceDto;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Turns narration text into cached audio (spec 4.5). Identical narration is synthesized once: the cache answers
 * repeats, and single flight makes simultaneous requests share one VoiceStudio call.
 */
@Service
@RequiredArgsConstructor
public class TtsService {

	private final VoiceStudioClient voiceStudio;
	private final AudioCacheService cache;
	private final VoiceStudioProperties settings;
	private final SingleFlight<AudioFile> singleFlight = new SingleFlight<>();

	public SynthesizeResponse synthesize(String text, String language) {
		requireWithinLimit(text);
		VoiceStudioRequest request = new VoiceStudioRequest(text, language, settings.defaultVoice());
		String hash = AudioKey.of(request);
		AudioFile audio = singleFlight.run(hash, () -> cachedOrSynthesized(hash, request));
		return new SynthesizeResponse(audio.url(), audio.durationMs());
	}

	public List<VoiceDto> voices() {
		return voiceStudio.voices();
	}

	private AudioFile cachedOrSynthesized(String hash, VoiceStudioRequest request) {
		return cache.find(hash).orElseGet(() -> cache.store(hash, voiceStudio.synthesize(request)));
	}

	private static void requireWithinLimit(String text) {
		if (text.length() > TtsLimits.MAX_TEXT_LENGTH) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED,
					"Narration text is longer than " + TtsLimits.MAX_TEXT_LENGTH + " characters.");
		}
	}
}
