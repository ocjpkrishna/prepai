package com.ascorp.prepai.common.redis;

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

/** Runs the counter scripts against the real Redis test database 15 (BUILD-DECISIONS.md, decision 4). */
@Tag("db")
class RedisCounterRedisTest {

	private static final int TEST_DATABASE = 15;
	private static final String KEY = "test:counter";

	private static LettuceConnectionFactory connections;
	private static StringRedisTemplate redis;
	private RedisCounter counters;

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
		counters = new RedisCounter(redis);
	}

	@AfterEach
	void clearKey() {
		redis.delete(KEY);
	}

	@Test
	void incrementCountsUpAndExpiresWithTheFirstIncrement() {
		assertThat(counters.increment(KEY, Duration.ofMinutes(5))).isEqualTo(1);
		assertThat(counters.increment(KEY, Duration.ofMinutes(5))).isEqualTo(2);

		assertThat(redis.getExpire(KEY)).isPositive().isLessThanOrEqualTo(Duration.ofMinutes(5).toSeconds());
	}

	@Test
	void decrementTakesOneBackOnlyWhileTheCounterExists() {
		counters.increment(KEY, Duration.ofMinutes(5));
		counters.increment(KEY, Duration.ofMinutes(5));

		counters.decrementIfPresent(KEY);
		counters.decrementIfPresent("test:missing");

		assertThat(redis.opsForValue().get(KEY)).isEqualTo("1");
		assertThat(redis.hasKey("test:missing")).isFalse();
	}
}
