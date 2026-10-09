package com.ascorp.prepai.quota.ratelimit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.quota.ratelimit.repository.RateLimitRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExtractLimiterTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final Instant QUARTER_PAST_TEN = Instant.parse("2026-10-08T10:15:00Z");

	@Mock
	private RateLimitRepository counters;

	private ExtractLimiter extracts;

	@BeforeEach
	void setUp() {
		extracts = new ExtractLimiter(counters, Clock.fixed(QUARTER_PAST_TEN, ZoneOffset.UTC));
	}

	@Test
	void theTwentiethPhotoOfTheHourIsAllowed() {
		when(counters.incrementExtract(USER_ID, QUARTER_PAST_TEN)).thenReturn(ExtractLimiter.MAX_EXTRACTS_PER_HOUR);

		assertThatCode(() -> extracts.assertWithinExtractLimit(USER_ID)).doesNotThrowAnyException();
	}

	@Test
	void the21stPhotoOfTheHourIsRateLimitedUntilTheHourEnds() {
		when(counters.incrementExtract(USER_ID, QUARTER_PAST_TEN)).thenReturn(ExtractLimiter.MAX_EXTRACTS_PER_HOUR + 1);

		assertThatThrownBy(() -> extracts.assertWithinExtractLimit(USER_ID))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getCode()).isEqualTo(ErrorCode.RATE_LIMITED);
					assertThat(exception.getRetryAfterSeconds()).isEqualTo(45 * 60);
				});
	}

	@Test
	void theHourlyKeyIsPassedToTheCounter() {
		when(counters.incrementExtract(any(), any())).thenReturn(1L);

		extracts.assertWithinExtractLimit(USER_ID);

		verify(counters).incrementExtract(USER_ID, QUARTER_PAST_TEN);
	}
}
