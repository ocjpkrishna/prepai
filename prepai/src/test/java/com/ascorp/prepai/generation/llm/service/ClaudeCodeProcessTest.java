package com.ascorp.prepai.generation.llm.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.generation.llm.model.ClaudeCodeProperties;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.nio.file.attribute.PosixFilePermissions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Runs the real process code against small shell scripts that stand in for the Claude Code program. */
class ClaudeCodeProcessTest {

	private static final String REPLY = "{\"type\":\"result\",\"result\":\"hello\"}";

	@TempDir
	Path scripts;

	@Test
	void theAnswerOfTheProgramIsReturnedAndTheQuestionIsSentOnStdin() throws IOException {
		Path seen = scripts.resolve("seen.txt");
		Path program = script("cat > '" + seen + "'; printf '%s' '" + REPLY + "'");

		String answer = process(program, Duration.ofSeconds(10)).run("system text", "the question");

		assertThat(answer).isEqualTo(REPLY);
		assertThat(Files.readString(seen)).isEqualTo("the question");
	}

	@Test
	void noToolsNoSettingsAndNoSessionAreAskedFor() throws IOException {
		Path args = scripts.resolve("args.txt");
		Path program = script("printf '%s\\n' \"$@\" > '" + args + "'; cat > /dev/null; printf '%s' '" + REPLY + "'");

		process(program, Duration.ofSeconds(10)).run("system text", "q");

		assertThat(Files.readAllLines(args)).contains("--tools", "--setting-sources", "--no-session-persistence",
				"--strict-mcp-config", "--disable-slash-commands", "system text");
	}

	@Test
	void aNonZeroExitIsAProviderError() throws IOException {
		Path program = script("cat > /dev/null; exit 3");

		assertThatThrownBy(() -> process(program, Duration.ofSeconds(10)).run("s", "q"))
				.isInstanceOfSatisfying(LlmProviderException.class,
						failure -> assertThat(failure.getReason()).isEqualTo(RetryReason.PROVIDER_ERROR));
	}

	@Test
	void anAnswerThatTakesTooLongIsATimeout() throws IOException {
		Path program = script("cat > /dev/null; sleep 5");

		assertThatThrownBy(() -> process(program, Duration.ofMillis(300)).run("s", "q"))
				.isInstanceOfSatisfying(LlmProviderException.class,
						failure -> assertThat(failure.getReason()).isEqualTo(RetryReason.TIMEOUT));
	}

	@Test
	void aMissingProgramIsAProviderError() throws IOException {
		ClaudeCodeProcess missing = new ClaudeCodeProcess(new ClaudeCodeProperties(
				scripts.resolve("nope").toString(), "sonnet", Duration.ofSeconds(5), 1));

		assertThatThrownBy(() -> missing.run("s", "q")).isInstanceOf(LlmProviderException.class);
	}

	private ClaudeCodeProcess process(Path program, Duration timeout) throws IOException {
		return new ClaudeCodeProcess(new ClaudeCodeProperties(program.toString(), "sonnet", timeout, 2));
	}

	private Path script(String body) throws IOException {
		Path file = Files.createTempFile(scripts, "fake-claude", ".sh");
		Files.writeString(file, "#!/bin/sh\n" + body + "\n");
		Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rwx------"));
		return file;
	}
}
