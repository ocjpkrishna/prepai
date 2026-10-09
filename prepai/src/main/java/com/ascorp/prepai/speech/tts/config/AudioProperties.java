package com.ascorp.prepai.speech.tts.config;

import java.nio.file.Path;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Audio cache settings (prepai.audio.*, spec 4.5): where files live, how long and how large, warm-up parallelism. */
@ConfigurationProperties("prepai.audio")
public record AudioProperties(Path dir, int ttlDays, int maxGb, int warmupConcurrency) {
}
