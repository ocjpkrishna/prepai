package com.ascorp.prepai.speech.tts.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.speech.tts.config.AudioProperties;
import com.ascorp.prepai.speech.tts.model.StoredAudio;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AudioCleanupServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-09T03:00:00Z");
	private static final int TTL_DAYS = 30;
	private static final long HALF_GB = 512L * 1024 * 1024;

	@Mock
	private AudioCacheService cache;

	private AudioCleanupService cleanup;

	@BeforeEach
	void setUp() {
		cleanup = new AudioCleanupService(cache, new AudioProperties(Path.of("unused"), TTL_DAYS, 1, 2),
				Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void filesUnusedForLongerThanTheTtlAreDeleted() {
		StoredAudio old = entry("old", 1, NOW.minusSeconds(31L * 24 * 3600));
		StoredAudio recent = entry("recent", 1, NOW.minusSeconds(3600));
		when(cache.entries()).thenReturn(List.of(old, recent));

		cleanup.cleanUp();

		verify(cache).delete(old.path());
		verify(cache, never()).delete(recent.path());
	}

	@Test
	void whenTheDirectoryIsOverTheSizeLimitTheLeastRecentlyUsedGoFirst() {
		StoredAudio oldest = entry("oldest", HALF_GB, NOW.minusSeconds(300));
		StoredAudio newer = entry("newer", HALF_GB, NOW.minusSeconds(200));
		StoredAudio newest = entry("newest", HALF_GB, NOW.minusSeconds(100));
		when(cache.entries()).thenReturn(List.of(newest, oldest, newer));

		cleanup.cleanUp();

		verify(cache).delete(oldest.path());
		verify(cache, never()).delete(newer.path());
		verify(cache, never()).delete(newest.path());
	}

	@Test
	void aDirectoryWithinTheSizeLimitKeepsEveryRecentFile() {
		StoredAudio small = entry("small", HALF_GB - 1, NOW.minusSeconds(100));
		StoredAudio other = entry("other", 1, NOW.minusSeconds(200));
		when(cache.entries()).thenReturn(List.of(small, other));

		cleanup.cleanUp();

		verify(cache, never()).delete(any());
	}

	private static StoredAudio entry(String name, long sizeBytes, Instant lastUsed) {
		return new StoredAudio(Path.of(name + ".wav"), sizeBytes, lastUsed);
	}
}
