package com.ascorp.prepai.speech.tts.service;

import com.ascorp.prepai.speech.tts.model.VoiceStudioRequest;
import com.ascorp.prepai.speech.tts.model.dto.VoiceDto;
import java.util.List;

/**
 * The local TTS server (VoiceStudio). The only class that talks to it; tests use a fake, because the server is
 * not part of this repository (spec 4.5, decision 6).
 */
public interface VoiceStudioClient {

	/** Returns the WAV bytes of one narration. Throws TTS_UNAVAILABLE when the server is down, slow or wrong. */
	byte[] synthesize(VoiceStudioRequest request);

	/** Lists the voices the server offers. Throws TTS_UNAVAILABLE when the server is down, slow or wrong. */
	List<VoiceDto> voices();
}
