package com.ascorp.prepai.speech.tts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.speech.tts.config.AudioProperties;
import com.ascorp.prepai.speech.tts.model.AudioFile;
import com.ascorp.prepai.speech.tts.model.StoredAudio;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class AudioCacheServiceTest {

	private static final String HASH = "a1b2c3";
	private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");

	@TempDir
	private Path dir;

	private AudioCacheService cache;

	@BeforeEach
	void setUp() {
		AudioProperties settings = new AudioProperties(dir, 30, 10, 2);
		cache = new AudioCacheService(settings, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void aMissingHashIsNotFound() {
		assertThat(cache.find(HASH)).isEmpty();
	}

	@Test
	void aStoredNarrationIsFoundWithItsDuration() {
		cache.store(HASH, WavFixtures.wav(WavFixtures.DEFAULT_MILLIS));

		assertThat(cache.find(HASH)).contains(new AudioFile(HASH, WavFixtures.DEFAULT_MILLIS));
	}

	@Test
	void aStoredFileLeavesNoTemporaryFileBehind() throws Exception {
		cache.store(HASH, WavFixtures.wav(WavFixtures.DEFAULT_MILLIS));

		try (var files = Files.list(dir)) {
			assertThat(files.map(file -> file.getFileName().toString())).containsExactly(HASH + ".wav");
		}
	}

	@Test
	void aHitMovesTheFileToTheNewestTime() throws Exception {
		cache.store(HASH, WavFixtures.wav(WavFixtures.DEFAULT_MILLIS));
		Files.setLastModifiedTime(dir.resolve(HASH + ".wav"), FileTime.from(Instant.EPOCH));

		cache.find(HASH);

		assertThat(cache.entries()).singleElement().extracting(StoredAudio::lastUsed).isEqualTo(NOW);
	}

	@Test
	void entriesListOnlyFinishedAudioFiles() throws Exception {
		cache.store(HASH, WavFixtures.wav(WavFixtures.DEFAULT_MILLIS));
		Files.writeString(dir.resolve("half-written.wav.tmp"), "partial");

		assertThat(cache.entries()).extracting(entry -> entry.path().getFileName().toString())
				.containsExactly(HASH + ".wav");
	}

	@Test
	void anAudioDirectoryThatDoesNotExistYetHasNoEntries() {
		AudioCacheService fresh = new AudioCacheService(
				new AudioProperties(dir.resolve("not-yet"), 30, 10, 2), Clock.fixed(NOW, ZoneOffset.UTC));

		assertThat(fresh.entries()).isEmpty();
	}

	@Test
	void aFileThatCannotBeWrittenIsTtsUnavailable() throws Exception {
		Path blocker = Files.createFile(dir.resolve("blocker"));
		AudioCacheService broken = new AudioCacheService(
				new AudioProperties(blocker, 30, 10, 2), Clock.fixed(NOW, ZoneOffset.UTC));

		assertThatThrownBy(() -> broken.store(HASH, WavFixtures.wav(WavFixtures.DEFAULT_MILLIS)))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getCode()).isEqualTo(ErrorCode.TTS_UNAVAILABLE));
	}

	@Test
	void deletingAFileRemovesIt() throws Exception {
		cache.store(HASH, WavFixtures.wav(WavFixtures.DEFAULT_MILLIS));

		cache.delete(dir.resolve(HASH + ".wav"));

		assertThat(cache.find(HASH)).isEmpty();
	}
}
