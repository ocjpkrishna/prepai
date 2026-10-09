package com.ascorp.prepai.speech.tts.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Turns on the background warm-up (@Async) and the nightly audio cleanup (@Scheduled). */
@Configuration
@EnableAsync
@EnableScheduling
public class TtsConfig {
}
