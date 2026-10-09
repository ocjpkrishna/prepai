package com.ascorp.prepai.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.config.SignupProperties;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.redis.HourWindow;
import com.ascorp.prepai.common.redis.RedisCounter;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SignupThrottleTest {

	private static final String IP = "203.0.113.7";
	private static final Instant QUARTER_PAST_TEN = Instant.parse("2026-10-08T10:15:00Z");
	private static final int LIMIT = 5;

	@Mock
	private RedisCounter counters;

	@Test
	void theFifthSignUpFromAnAddressInAnHourIsAllowed() {
		when(counters.increment(anyString(), any())).thenReturn((long) LIMIT);

		assertThatCode(() -> throttle().assertWithinSignupLimit(IP)).doesNotThrowAnyException();
	}

	@Test
	void theSixthSignUpInAnHourIsRateLimitedUntilTheHourEnds() {
		when(counters.increment(anyString(), any())).thenReturn((long) LIMIT + 1);

		assertThatThrownBy(() -> throttle().assertWithinSignupLimit(IP))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getCode()).isEqualTo(ErrorCode.RATE_LIMITED);
					assertThat(exception.getRetryAfterSeconds()).isEqualTo(45 * 60);
				});
	}

	@Test
	void theCounterIsKeyedByAddressAndHourForAnHour() {
		when(counters.increment(anyString(), any())).thenReturn(1L);

		throttle().assertWithinSignupLimit(IP);

		String hour = String.valueOf(HourWindow.start(QUARTER_PAST_TEN).getEpochSecond());
		verify(counters).increment("signup:" + IP + ":" + hour, Duration.ofHours(1));
	}

	private SignupThrottle throttle() {
		return new SignupThrottle(counters, new SignupProperties(LIMIT),
				Clock.fixed(QUARTER_PAST_TEN, ZoneOffset.UTC));
	}
}
