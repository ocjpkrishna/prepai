package com.ascorp.prepai.speech.tts.model.dto;

/** One voice of the TTS server, as GET /api/v1/tts/voices lists it. */
public record VoiceDto(String id, String name, String language) {
}
