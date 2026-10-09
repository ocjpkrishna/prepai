package com.ascorp.prepai.quota.ratelimit.repository;

import com.ascorp.prepai.common.redis.HourWindow;
import com.ascorp.prepai.common.redis.RedisCounter;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/** The Redis counters behind the limits: one key per student and day, per burst window, and per hour (spec 5.3). */
@Repository
@RequiredArgsConstructor
public class RateLimitRepository {

	private static final Duration ONE_DAY = Duration.ofDays(1);

	private final RedisCounter counters;

	public long incrementDaily(UUID userId, LocalDate day) {
		return counters.increment(dailyKey(userId, day), ONE_DAY);
	}

	public void releaseDaily(UUID userId, LocalDate day) {
		counters.decrementIfPresent(dailyKey(userId, day));
	}

	public long incrementBurst(UUID userId, Duration window) {
		return counters.increment("ratelimit:burst:" + userId, window);
	}

	public long incrementExtract(UUID userId, Instant now) {
		String hour = String.valueOf(HourWindow.start(now).getEpochSecond());
		return counters.increment("ratelimit:extract:" + userId + ":" + hour, HourWindow.LENGTH);
	}

	private static String dailyKey(UUID userId, LocalDate day) {
		return "ratelimit:" + userId + ":" + day;
	}
}
