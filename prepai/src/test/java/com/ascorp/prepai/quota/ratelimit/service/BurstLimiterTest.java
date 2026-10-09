package com.ascorp.prepai.quota.ratelimit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.quota.ratelimit.repository.RateLimitRepository;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BurstLimiterTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");

	@Mock
	private RateLimitRepository counters;

	private BurstLimiter burst;

	@BeforeEach
	void setUp() {
		burst = new BurstLimiter(counters);
	}

	@Test
	void attemptsUpToTheWindowLimitAreAllowed() {
		when(counters.incrementBurst(USER_ID, BurstLimiter.WINDOW)).thenReturn(BurstLimiter.MAX_ATTEMPTS_PER_WINDOW);

		assertThatCode(() -> burst.assertWithinBurst(USER_ID)).doesNotThrowAnyException();
	}

	@Test
	void anAttemptPastTheWindowLimitIsRateLimitedWithACountdown() {
		long overLimit = BurstLimiter.MAX_ATTEMPTS_PER_WINDOW + 1;
		when(counters.incrementBurst(USER_ID, BurstLimiter.WINDOW)).thenReturn(overLimit);

		assertThatThrownBy(() -> burst.assertWithinBurst(USER_ID))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getCode()).isEqualTo(ErrorCode.RATE_LIMITED);
					assertThat(exception.getRetryAfterSeconds()).isEqualTo(Duration.ofMinutes(1).toSeconds());
				});
	}
}
