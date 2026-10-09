package com.ascorp.prepai.speech.tts.model;

import java.nio.file.Path;
import java.time.Instant;

/** A file in the audio cache as the cleanup sees it: its size, and when it was last used (its modification time). */
public record StoredAudio(Path path, long sizeBytes, Instant lastUsed) {
}
