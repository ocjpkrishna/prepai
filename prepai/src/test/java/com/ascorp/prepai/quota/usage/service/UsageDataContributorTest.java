package com.ascorp.prepai.quota.usage.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.quota.usage.model.dto.UsageEntryResponse;
import com.ascorp.prepai.quota.usage.model.entity.UsageLog;
import com.ascorp.prepai.quota.usage.repository.UsageLogRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsageDataContributorTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final UUID LESSON_ID = UUID.fromString("5b1c2d3e-4f50-4a61-8b72-9c83d94e5f06");
	private static final LocalDate DAY = LocalDate.of(2026, 10, 8);
	private static final Instant CREATED = Instant.parse("2026-10-08T10:00:00Z");

	@Mock
	private UsageLogRepository usageLogs;

	@InjectMocks
	private UsageDataContributor contributor;

	@Test
	void theSectionIsCalledUsage() {
		assertThat(contributor.section()).isEqualTo("usage");
	}

	@Test
	void exportListsEverySessionOfTheStudent() {
		UsageLog log = UsageLog.builder()
				.id(UUID.randomUUID())
				.userId(USER_ID)
				.lessonId(LESSON_ID)
				.sessionDate(DAY)
				.durationSeconds(240)
				.createdAt(CREATED)
				.build();
		when(usageLogs.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(log));

		assertThat(contributor.export(USER_ID))
				.isEqualTo(List.of(new UsageEntryResponse(LESSON_ID, DAY, 240, CREATED)));
	}

	@Test
	void eraseDeletesEveryUsageRowOfTheStudent() {
		contributor.erase(USER_ID);

		verify(usageLogs).deleteByUserId(USER_ID);
	}
}
