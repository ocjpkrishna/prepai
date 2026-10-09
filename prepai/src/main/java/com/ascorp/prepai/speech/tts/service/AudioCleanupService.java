package com.ascorp.prepai.speech.tts.service;

import com.ascorp.prepai.speech.tts.config.AudioProperties;
import com.ascorp.prepai.speech.tts.model.StoredAudio;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * The retention rules of spec 4.5: first delete files unused for AUDIO_TTL_DAYS, then, if the directory is still
 * over AUDIO_MAX_GB, delete the least recently used files until it is not.
 */
@Service
@RequiredArgsConstructor
public class AudioCleanupService {

	private static final long BYTES_PER_GB = 1024L * 1024 * 1024;

	private final AudioCacheService cache;
	private final AudioProperties audio;
	private final Clock clock;

	public void cleanUp() {
		Instant cutoff = clock.instant().minus(Duration.ofDays(audio.ttlDays()));
		List<StoredAudio> remaining = deleteUnusedSince(cache.entries(), cutoff);
		evictLeastRecentlyUsed(remaining, audio.maxGb() * BYTES_PER_GB);
	}

	private List<StoredAudio> deleteUnusedSince(List<StoredAudio> entries, Instant cutoff) {
		List<StoredAudio> remaining = new ArrayList<>();
		for (StoredAudio entry : entries) {
			if (entry.lastUsed().isBefore(cutoff)) {
				cache.delete(entry.path());
			} else {
				remaining.add(entry);
			}
		}
		return remaining;
	}

	private void evictLeastRecentlyUsed(List<StoredAudio> entries, long limitBytes) {
		long totalBytes = entries.stream().mapToLong(StoredAudio::sizeBytes).sum();
		List<StoredAudio> oldestFirst = entries.stream()
				.sorted(Comparator.comparing(StoredAudio::lastUsed))
				.toList();
		for (StoredAudio entry : oldestFirst) {
			if (totalBytes <= limitBytes) {
				return;
			}
			cache.delete(entry.path());
			totalBytes -= entry.sizeBytes();
		}
	}
}
