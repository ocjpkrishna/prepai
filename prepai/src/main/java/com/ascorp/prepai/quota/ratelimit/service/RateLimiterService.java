package com.ascorp.prepai.quota.ratelimit.service;

import com.ascorp.prepai.account.user.service.UserService;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Plan;
import com.ascorp.prepai.quota.ratelimit.model.SessionReservation;
import com.ascorp.prepai.quota.ratelimit.repository.RateLimitRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Reserves a lesson session before generation and counts it per student day (spec 4.2, 5.3). */
@Service
@RequiredArgsConstructor
public class RateLimiterService {

	/** Students' days start at midnight in India, so the daily limit resets then. */
	private static final ZoneId STUDENT_ZONE = ZoneId.of("Asia/Kolkata");

	private final UserService users;
	private final RateLimitRepository counters;
	private final BurstLimiter burst;
	private final Clock clock;

	/** Holds one session for today. Commit it once the lesson is delivered; otherwise closing it gives it back. */
	public SessionReservation reserveSession(UUID userId) {
		burst.assertWithinBurst(userId);
		Plan plan = users.currentPlan(userId);
		LocalDate today = studentNow().toLocalDate();
		long reserved = counters.incrementDaily(userId, today);
		if (exceedsDailyLimit(plan, reserved)) {
			counters.releaseDaily(userId, today);
			throw new ApiException(ErrorCode.DAILY_LIMIT_REACHED, ErrorCode.DAILY_LIMIT_REACHED.getDefaultMessage(),
					secondsUntilNextStudentDay());
		}
		return new SessionReservation(() -> counters.releaseDaily(userId, today));
	}

	/** Unlimited plans (null) are never stopped, but their sessions are still counted. */
	private static boolean exceedsDailyLimit(Plan plan, long reserved) {
		Integer limit = plan.getSessionsPerDay();
		return limit != null && reserved > limit;
	}

	private long secondsUntilNextStudentDay() {
		ZonedDateTime now = studentNow();
		return Duration.between(now, now.toLocalDate().plusDays(1).atStartOfDay(STUDENT_ZONE)).toSeconds();
	}

	private ZonedDateTime studentNow() {
		return ZonedDateTime.now(clock.withZone(STUDENT_ZONE));
	}
}
