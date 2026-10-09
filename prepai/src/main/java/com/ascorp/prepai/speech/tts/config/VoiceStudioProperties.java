package com.ascorp.prepai.speech.tts.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Where the local TTS server is and which voice narrates (prepai.voicestudio.*, spec 4.5). */
@ConfigurationProperties("prepai.voicestudio")
public record VoiceStudioProperties(String apiUrl, String defaultVoice) {
}
