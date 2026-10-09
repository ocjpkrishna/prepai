package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.generation.llm.model.ClaudeCodeProperties;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Runs one Claude Code question as a child process: no tools, no project settings, no session kept, and an empty
 * working folder, so the model can only answer the text it is given. The answer comes back as Claude Code's JSON.
 */
@Component
@ConditionalOnProperty(prefix = "prepai.llm", name = "provider", havingValue = "claude-code")
public class ClaudeCodeProcess {

	private static final String NO_MCP_SERVERS = "{\"mcpServers\":{}}";
	private static final long SLOT_WAIT_SECONDS = 30;

	private final ClaudeCodeProperties settings;
	private final Semaphore slots;
	private final Path workDir;

	public ClaudeCodeProcess(ClaudeCodeProperties settings) throws IOException {
		this.settings = settings;
		this.slots = new Semaphore(settings.maxConcurrent(), true);
		this.workDir = Files.createTempDirectory("prepai-claude-code");
	}

	public String run(String systemPrompt, String userPrompt) {
		acquireSlot();
		try {
			return execute(systemPrompt, userPrompt);
		} finally {
			slots.release();
		}
	}

	private String execute(String systemPrompt, String userPrompt) {
		Path output = null;
		try {
			output = Files.createTempFile(workDir, "reply", ".json");
			Process process = start(systemPrompt, output);
			feed(process, userPrompt);
			awaitSuccess(process);
			return Files.readString(output, StandardCharsets.UTF_8);
		} catch (IOException failure) {
			throw new LlmProviderException(RetryReason.PROVIDER_ERROR, failure);
		} finally {
			deleteQuietly(output);
		}
	}

	private Process start(String systemPrompt, Path output) throws IOException {
		return new ProcessBuilder(command(systemPrompt))
				.directory(workDir.toFile())
				.redirectOutput(output.toFile())
				.redirectError(ProcessBuilder.Redirect.DISCARD)
				.start();
	}

	private List<String> command(String systemPrompt) {
		return List.of(settings.executable(), "-p", "--model", settings.model(), "--output-format", "json",
				"--tools", "", "--system-prompt", systemPrompt, "--setting-sources", "",
				"--no-session-persistence", "--strict-mcp-config", "--mcp-config", NO_MCP_SERVERS,
				"--disable-slash-commands");
	}

	private static void feed(Process process, String userPrompt) throws IOException {
		try (var stdin = process.getOutputStream()) {
			stdin.write(userPrompt.getBytes(StandardCharsets.UTF_8));
		}
	}

	private void awaitSuccess(Process process) {
		try {
			if (!process.waitFor(settings.timeout().toMillis(), TimeUnit.MILLISECONDS)) {
				process.destroyForcibly();
				throw new LlmProviderException(RetryReason.TIMEOUT, new TimeoutException("Claude Code timed out"));
			}
		} catch (InterruptedException interrupted) {
			process.destroyForcibly();
			Thread.currentThread().interrupt();
			throw new LlmProviderException(RetryReason.PROVIDER_ERROR, interrupted);
		}
		if (process.exitValue() != 0) {
			throw new LlmProviderException(RetryReason.PROVIDER_ERROR,
					new IllegalStateException("Claude Code exited with code " + process.exitValue()));
		}
	}

	private void acquireSlot() {
		try {
			if (!slots.tryAcquire(SLOT_WAIT_SECONDS, TimeUnit.SECONDS)) {
				throw new LlmProviderException(RetryReason.RATE_LIMITED,
						new TimeoutException("No free Claude Code slot"));
			}
		} catch (InterruptedException interrupted) {
			Thread.currentThread().interrupt();
			throw new LlmProviderException(RetryReason.PROVIDER_ERROR, interrupted);
		}
	}

	private static void deleteQuietly(Path file) {
		try {
			if (file != null) {
				Files.deleteIfExists(file);
			}
		} catch (IOException ignored) {
			// a leftover temp file in our own folder is harmless
		}
	}
}
