package com.ascorp.prepai.generation.cache.repository;

import java.time.Duration;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

/** The Redis strings behind `lesson:cache:{inputHash}` (spec 5.3). Holds JSON text; the service converts it. */
@Repository
@RequiredArgsConstructor
public class LessonCacheRepository {

	private final StringRedisTemplate redis;

	public Optional<String> find(String key) {
		return Optional.ofNullable(redis.opsForValue().get(key));
	}

	public void save(String key, String json, Duration ttl) {
		redis.opsForValue().set(key, json, ttl);
	}

	public void delete(String key) {
		redis.delete(key);
	}
}
