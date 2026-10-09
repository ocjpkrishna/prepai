package com.ascorp.prepai.generation.cache.service;

import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.cache.repository.LessonCacheRepository;
import java.security.MessageDigest;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * The exact-match cache (spec 6.3, 5.3): an identical request gets the lesson the student already saw, for 7 days.
 * The key hashes the whole request as JSON, so two different requests can never share a key.
 */
@Service
@RequiredArgsConstructor
public class LessonCacheService {

	private static final String KEY_PREFIX = "lesson:cache:";
	private static final Duration TTL = Duration.ofDays(7);
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final LessonCacheRepository cache;

	public Optional<LessonResponse> find(LessonRequest request) {
		return cache.find(keyOf(request)).flatMap(LessonCacheService::readLesson);
	}

	/** An entry written by an older version of the contract is a miss, not a failed request. */
	private static Optional<LessonResponse> readLesson(String json) {
		try {
			return Optional.ofNullable(JSON.readValue(json, LessonResponse.class));
		} catch (JacksonException e) {
			return Optional.empty();
		}
	}

	/** Stores a lesson that has passed validation. Callers must never store anything else. */
	public void put(LessonRequest request, LessonResponse lesson) {
		cache.save(keyOf(request), JSON.writeValueAsString(lesson), TTL);
	}

	/** Removes the lesson for this request, for example after the verifier corrects it. */
	public void evict(LessonRequest request) {
		cache.delete(keyOf(request));
	}

	private static String keyOf(LessonRequest request) {
		return KEY_PREFIX + sha256Hex(JSON.writeValueAsString(request));
	}

	private static String sha256Hex(String text) {
		try {
			byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
			return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
		} catch (NoSuchAlgorithmException missing) {
			throw new IllegalStateException("Every Java runtime provides SHA-256", missing);
		}
	}
}
