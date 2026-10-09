package com.ascorp.prepai.generation.llm.model;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the generation pipeline (`prepai.llm.*`, spec 10.3): which provider, attempts and retry pause. */
@ConfigurationProperties("prepai.llm")
public record LlmProperties(String provider, int maxAttempts, Duration retryBackoff) {
}
