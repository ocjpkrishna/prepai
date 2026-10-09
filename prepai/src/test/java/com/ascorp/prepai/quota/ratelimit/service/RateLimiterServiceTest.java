package com.ascorp.prepai.quota.ratelimit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.user.service.UserService;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Plan;
import com.ascorp.prepai.quota.ratelimit.model.SessionReservation;
import com.ascorp.prepai.quota.ratelimit.repository.RateLimitRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	// 2026-10-08 10:00 UTC is 15:30 in India, so the student's day is 2026-10-08.
	private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
	private static final LocalDate STUDENT_DAY = LocalDate.of(2026, 10, 8);

	@Mock
	private UserService users;

	@Mock
	private RateLimitRepository counters;

	@Mock
	private BurstLimiter burst;

	private RateLimiterService limiter;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		limiter = new RateLimiterService(users, counters, burst, clock);
	}

	@Test
	void aReservationWithinTheDailyLimitIsGranted() {
		givenPlan(Plan.FREE);
		when(counters.incrementDaily(USER_ID, STUDENT_DAY)).thenReturn(3L);

		assertThatCode(() -> limiter.reserveSession(USER_ID).close()).doesNotThrowAnyException();
	}

	@Test
	void theFourthFreeSessionOfTheDayIsStopped() {
		givenPlan(Plan.FREE);
		when(counters.incrementDaily(USER_ID, STUDENT_DAY)).thenReturn(4L);

		assertThatThrownBy(() -> limiter.reserveSession(USER_ID))
				.isInstanceOfSatisfying(ApiException.class, exception -> {
					assertThat(exception.getCode()).isEqualTo(ErrorCode.DAILY_LIMIT_REACHED);
					assertThat(exception.getRetryAfterSeconds()).isEqualTo(8 * 3600 + 30 * 60);
				});
	}

	@Test
	void aStoppedReservationGivesTheCountBack() {
		givenPlan(Plan.FREE);
		when(counters.incrementDaily(USER_ID, STUDENT_DAY)).thenReturn(4L);

		assertThatThrownBy(() -> limiter.reserveSession(USER_ID)).isInstanceOf(ApiException.class);

		verify(counters).releaseDaily(USER_ID, STUDENT_DAY);
	}

	@Test
	void aClosedReservationReleasesTheSession() {
		givenPlan(Plan.PRO);
		when(counters.incrementDaily(USER_ID, STUDENT_DAY)).thenReturn(1L);

		limiter.reserveSession(USER_ID).close();

		verify(counters).releaseDaily(USER_ID, STUDENT_DAY);
	}

	@Test
	void aCommittedReservationKeepsTheSession() {
		givenPlan(Plan.PRO);
		when(counters.incrementDaily(USER_ID, STUDENT_DAY)).thenReturn(1L);

		try (SessionReservation session = limiter.reserveSession(USER_ID)) {
			session.commit();
		}

		verify(counters, never()).releaseDaily(any(), any());
	}

	@Test
	void unlimitedPlanIsNeverStoppedButStillCounted() {
		givenPlan(Plan.PRO_PLUS);
		when(counters.incrementDaily(USER_ID, STUDENT_DAY)).thenReturn(500L);

		assertThatCode(() -> limiter.reserveSession(USER_ID).close()).doesNotThrowAnyException();
	}

	@Test
	void everyAttemptPassesTheBurstCheckFirst() {
		givenPlan(Plan.FREE);
		when(counters.incrementDaily(USER_ID, STUDENT_DAY)).thenReturn(1L);

		limiter.reserveSession(USER_ID).close();

		verify(burst).assertWithinBurst(USER_ID);
	}

	private void givenPlan(Plan plan) {
		when(users.currentPlan(USER_ID)).thenReturn(plan);
	}
}
