package com.ascorp.prepai.quota.ratelimit.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.redis.HourWindow;
import com.ascorp.prepai.quota.ratelimit.repository.RateLimitRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Caps how many photos a student may send for reading each hour (spec 4.2: 20 requests per hour). */
@Service
@RequiredArgsConstructor
public class ExtractLimiter {

	static final long MAX_EXTRACTS_PER_HOUR = 20;

	private final RateLimitRepository counters;
	private final Clock clock;

	/** Counts one image-extract call and refuses it once the hour's allowance is used. */
	public void assertWithinExtractLimit(UUID userId) {
		Instant now = clock.instant();
		if (counters.incrementExtract(userId, now) > MAX_EXTRACTS_PER_HOUR) {
			throw new ApiException(ErrorCode.RATE_LIMITED,
					"You've reached the photo-reading limit for this hour.", HourWindow.secondsLeft(now));
		}
	}
}
