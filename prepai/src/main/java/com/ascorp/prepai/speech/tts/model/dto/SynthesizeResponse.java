package com.ascorp.prepai.speech.tts.model.dto;

/** Answer of POST /api/v1/tts/synthesize: where the audio is and how long it plays (spec 4.5). */
public record SynthesizeResponse(String audioUrl, long durationMs) {
}
