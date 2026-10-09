package com.ascorp.prepai.common.redis;

import java.time.Duration;
import java.util.List;
import java.util.Objects;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Counters in Redis that stay correct under concurrency (spec 5.3). Each script runs as one step on the server,
 * so a counter never exists without its expiry, even if the app stops between two commands.
 */
@Component
@RequiredArgsConstructor
public class RedisCounter {

	private static final RedisScript<Long> INCREMENT_WITH_EXPIRY = RedisScript.of("""
			local count = redis.call('INCR', KEYS[1])
			if count == 1 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end
			return count
			""", Long.class);

	private static final RedisScript<Long> DECREMENT_IF_PRESENT = RedisScript.of("""
			if redis.call('EXISTS', KEYS[1]) == 1 then return redis.call('DECR', KEYS[1]) end
			return 0
			""", Long.class);

	private final StringRedisTemplate redis;

	/** Adds one to the counter and returns the new value. The expiry is set when the counter is created. */
	public long increment(String key, Duration ttl) {
		Long count = redis.execute(INCREMENT_WITH_EXPIRY, List.of(key), String.valueOf(ttl.toSeconds()));
		return Objects.requireNonNull(count, "Redis returned no value for the counter");
	}

	/** Takes one back from the counter, if the counter still exists. */
	public void decrementIfPresent(String key) {
		redis.execute(DECREMENT_IF_PRESENT, List.of(key));
	}
}
