package com.ascorp.prepai.quota.usage.service;

import com.ascorp.prepai.account.user.service.UserService;
import com.ascorp.prepai.common.model.enums.Plan;
import com.ascorp.prepai.quota.usage.model.dto.UsageResponse;
import com.ascorp.prepai.quota.usage.model.entity.UsageLog;
import com.ascorp.prepai.quota.usage.repository.UsageLogRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Records the sessions a student really received and summarises today's use (spec 4.3, 5.1). */
@Service
@RequiredArgsConstructor
public class UsageService {

	/** Students' days start at midnight in India, as in the rate limiter (BUILD-DECISIONS.md). */
	private static final ZoneId STUDENT_ZONE = ZoneId.of("Asia/Kolkata");

	private final UserService users;
	private final UsageLogRepository usageLogs;
	private final Supplier<UUID> ids;
	private final Clock clock;

	/** Records one delivered session, dated by the student's day. Lesson calls this once the lesson is stored. */
	@Transactional
	public void recordSession(UUID userId, UUID lessonId, int durationSeconds) {
		usageLogs.save(UsageLog.builder()
				.id(ids.get())
				.userId(userId)
				.lessonId(lessonId)
				.sessionDate(studentToday())
				.durationSeconds(durationSeconds)
				.createdAt(clock.instant())
				.build());
	}

	@Transactional(readOnly = true)
	public UsageResponse summary(UUID userId) {
		Plan plan = users.currentPlan(userId);
		long sessionsToday = usageLogs.countByUserIdAndSessionDate(userId, studentToday());
		return new UsageResponse(plan, sessionsToday, plan.getSessionsPerDay(), plan.getMaxSessionMinutes());
	}

	private LocalDate studentToday() {
		return LocalDate.now(clock.withZone(STUDENT_ZONE));
	}
}
