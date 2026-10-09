package com.ascorp.prepai.speech.tts.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.speech.tts.config.AudioProperties;
import com.ascorp.prepai.speech.tts.model.AudioFile;
import com.ascorp.prepai.speech.tts.model.StoredAudio;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * The audio files in AUDIO_DIR, one per narration hash (spec 4.5). A hit touches the file, so its modification time
 * is the last time it was used, which is what the cleanup goes by. Writes go to a temporary file first, so nginx
 * never serves half a file. Disk failures become TTS_UNAVAILABLE.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AudioCacheService {

	private static final String EXTENSION = ".wav";
	private static final String TEMP_SUFFIX = ".tmp";

	private final AudioProperties audio;
	private final Clock clock;

	public Optional<AudioFile> find(String hash) {
		Path file = fileOf(hash);
		if (Files.notExists(file)) {
			return Optional.empty();
		}
		try {
			Files.setLastModifiedTime(file, FileTime.from(clock.instant()));
			return Optional.of(new AudioFile(hash, durationOf(file)));
		} catch (IOException e) {
			throw unavailable(e);
		}
	}

	public AudioFile store(String hash, byte[] wav) {
		Path file = fileOf(hash);
		Path temp = file.resolveSibling(file.getFileName() + TEMP_SUFFIX);
		try {
			Files.createDirectories(audio.dir());
			Files.write(temp, wav);
			Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
		} catch (IOException e) {
			discard(temp);
			throw unavailable(e);
		}
		return new AudioFile(hash, WavFormat.durationMillis(wav));
	}

	/** Every finished audio file with its size and last use. An empty directory (not yet created) has none. */
	public List<StoredAudio> entries() {
		if (Files.notExists(audio.dir())) {
			return List.of();
		}
		List<StoredAudio> entries = new ArrayList<>();
		try (Stream<Path> files = Files.list(audio.dir())) {
			for (Path file : files.filter(AudioCacheService::isFinishedAudio).toList()) {
				entries.add(describe(file));
			}
		} catch (IOException e) {
			throw unavailable(e);
		}
		return entries;
	}

	/** Removes a file. A failed delete is logged and left for the next night; it never stops the cleanup. */
	public void delete(Path file) {
		try {
			Files.deleteIfExists(file);
		} catch (IOException e) {
			log.warn("Could not delete audio file {}: {}", file.getFileName(), e.getMessage());
		}
	}

	private void discard(Path temp) {
		try {
			Files.deleteIfExists(temp);
		} catch (IOException e) {
			log.warn("Could not delete temporary audio file {}: {}", temp.getFileName(), e.getMessage());
		}
	}

	private Path fileOf(String hash) {
		return audio.dir().resolve(hash + EXTENSION);
	}

	private long durationOf(Path file) throws IOException {
		return WavFormat.durationMillis(Files.readAllBytes(file));
	}

	private static StoredAudio describe(Path file) throws IOException {
		BasicFileAttributes attributes = Files.readAttributes(file, BasicFileAttributes.class);
		return new StoredAudio(file, attributes.size(), attributes.lastModifiedTime().toInstant());
	}

	private static boolean isFinishedAudio(Path file) {
		return file.getFileName().toString().endsWith(EXTENSION);
	}

	private static ApiException unavailable(IOException cause) {
		log.warn("Audio storage failed: {}", cause.getMessage());
		return new ApiException(ErrorCode.TTS_UNAVAILABLE);
	}
}
