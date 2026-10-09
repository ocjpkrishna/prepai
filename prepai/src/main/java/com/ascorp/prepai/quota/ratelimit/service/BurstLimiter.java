package com.ascorp.prepai.quota.ratelimit.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.quota.ratelimit.repository.RateLimitRepository;
import java.time.Duration;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Stops a student from starting lessons in quick succession (RATE_LIMITED, spec 4.7). Every attempt counts. */
@Service
@RequiredArgsConstructor
public class BurstLimiter {

	static final long MAX_ATTEMPTS_PER_WINDOW = 5;
	static final Duration WINDOW = Duration.ofMinutes(1);

	private final RateLimitRepository counters;

	public void assertWithinBurst(UUID userId) {
		if (counters.incrementBurst(userId, WINDOW) > MAX_ATTEMPTS_PER_WINDOW) {
			throw new ApiException(ErrorCode.RATE_LIMITED, ErrorCode.RATE_LIMITED.getDefaultMessage(),
					WINDOW.toSeconds());
		}
	}
}
