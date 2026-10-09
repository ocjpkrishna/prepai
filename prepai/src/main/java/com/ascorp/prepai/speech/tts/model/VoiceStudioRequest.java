package com.ascorp.prepai.speech.tts.model;

/** What the TTS server needs to synthesize one narration at normal speed. */
public record VoiceStudioRequest(String text, String language, String voice) {
}
