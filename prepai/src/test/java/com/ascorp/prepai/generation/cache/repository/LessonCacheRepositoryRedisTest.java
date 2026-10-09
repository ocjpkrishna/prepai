package com.ascorp.prepai.generation.cache.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;

/** Runs the cache strings against the real Redis test database 15 (BUILD-DECISIONS.md, decision 4). */
@Tag("db")
class LessonCacheRepositoryRedisTest {

	private static final int TEST_DATABASE = 15;
	private static final String KEY = "lesson:cache:test-entry";

	private static LettuceConnectionFactory connections;
	private static StringRedisTemplate redis;
	private LessonCacheRepository cache;

	@BeforeAll
	static void connect() {
		RedisStandaloneConfiguration configuration = new RedisStandaloneConfiguration("localhost", 6379);
		configuration.setDatabase(TEST_DATABASE);
		connections = new LettuceConnectionFactory(configuration);
		connections.afterPropertiesSet();
		redis = new StringRedisTemplate(connections);
	}

	@AfterAll
	static void disconnect() {
		connections.destroy();
	}

	@BeforeEach
	void setUp() {
		cache = new LessonCacheRepository(redis);
	}

	@AfterEach
	void clearKey() {
		redis.delete(KEY);
	}

	@Test
	void savedJsonIsFoundUntilItExpires() {
		cache.save(KEY, "{\"title\":\"x\"}", Duration.ofMinutes(5));

		assertThat(cache.find(KEY)).contains("{\"title\":\"x\"}");
		assertThat(redis.getExpire(KEY)).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(5).toSeconds());
	}

	@Test
	void deleteRemovesTheEntry() {
		cache.save(KEY, "{}", Duration.ofMinutes(5));

		cache.delete(KEY);

		assertThat(cache.find(KEY)).isEmpty();
	}

	@Test
	void findIsEmptyForAnUnknownKey() {
		assertThat(cache.find("lesson:cache:missing-entry")).isEmpty();
	}
}
