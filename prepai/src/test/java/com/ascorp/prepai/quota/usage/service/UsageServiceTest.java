package com.ascorp.prepai.quota.usage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.user.service.UserService;
import com.ascorp.prepai.common.model.enums.Plan;
import com.ascorp.prepai.quota.usage.model.dto.UsageResponse;
import com.ascorp.prepai.quota.usage.model.entity.UsageLog;
import com.ascorp.prepai.quota.usage.repository.UsageLogRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final UUID LESSON_ID = UUID.fromString("5b1c2d3e-4f50-4a61-8b72-9c83d94e5f06");
	private static final UUID ENTRY_ID = UUID.fromString("7d8e9f00-1a2b-4c3d-8e4f-5a6b7c8d9e0f");
	// 2026-10-08 19:00 UTC is 00:30 on 2026-10-09 in India, so the student's day has already changed.
	private static final Instant NOW = Instant.parse("2026-10-08T19:00:00Z");
	private static final LocalDate STUDENT_DAY = LocalDate.of(2026, 10, 9);

	@Mock
	private UserService users;

	@Mock
	private UsageLogRepository usageLogs;

	private UsageService usage;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		usage = new UsageService(users, usageLogs, () -> ENTRY_ID, clock);
	}

	@Test
	void aRecordedSessionIsDatedByTheStudentsDay() {
		usage.recordSession(USER_ID, LESSON_ID, 240);

		ArgumentCaptor<UsageLog> saved = ArgumentCaptor.forClass(UsageLog.class);
		verify(usageLogs).save(saved.capture());
		assertThat(saved.getValue())
				.extracting(UsageLog::getId, UsageLog::getUserId, UsageLog::getLessonId, UsageLog::getSessionDate,
						UsageLog::getDurationSeconds, UsageLog::getCreatedAt)
				.containsExactly(ENTRY_ID, USER_ID, LESSON_ID, STUDENT_DAY, 240, NOW);
	}

	@Test
	void theSummaryCountsTodaysSessionsAgainstThePlanLimit() {
		when(users.currentPlan(USER_ID)).thenReturn(Plan.FREE);
		when(usageLogs.countByUserIdAndSessionDate(USER_ID, STUDENT_DAY)).thenReturn(2L);

		assertThat(usage.summary(USER_ID)).isEqualTo(new UsageResponse(Plan.FREE, 2L, 3, 5));
	}

	@Test
	void theSummaryHasNoSessionLimitForProPlus() {
		when(users.currentPlan(USER_ID)).thenReturn(Plan.PRO_PLUS);
		when(usageLogs.countByUserIdAndSessionDate(any(), any())).thenReturn(7L);

		assertThat(usage.summary(USER_ID)).isEqualTo(new UsageResponse(Plan.PRO_PLUS, 7L, null, 30));
	}
}
