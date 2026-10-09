package com.ascorp.prepai.generation.llm.model;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Settings of the `claude-code` provider (`prepai.llm.claude-code.*`): the Claude Code program that answers, the
 * model it uses, how long one answer may take and how many may run at once.
 */
@ConfigurationProperties("prepai.llm.claude-code")
public record ClaudeCodeProperties(String executable, String model, Duration timeout, int maxConcurrent) {
}
