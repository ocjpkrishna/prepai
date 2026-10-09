package com.ascorp.prepai.quota.usage.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One lesson session the student really received (spec 5.1). These rows are personal data for export and purge. */
@Entity
@Table(name = "usage_log")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class UsageLog {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID userId;

	private UUID lessonId;

	@Column(nullable = false)
	private LocalDate sessionDate;

	@Column(nullable = false)
	private int durationSeconds;

	@Column(nullable = false)
	private Instant createdAt;
}
