package com.ascorp.prepai.quota.usage.model.dto;

import com.ascorp.prepai.quota.usage.model.entity.UsageLog;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** One delivered session in the data download (spec 2.7). */
public record UsageEntryResponse(UUID lessonId, LocalDate sessionDate, int durationSeconds, Instant createdAt) {

	public static UsageEntryResponse from(UsageLog log) {
		return new UsageEntryResponse(log.getLessonId(), log.getSessionDate(), log.getDurationSeconds(),
				log.getCreatedAt());
	}
}
