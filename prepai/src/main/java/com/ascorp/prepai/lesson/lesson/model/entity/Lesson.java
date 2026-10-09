package com.ascorp.prepai.lesson.lesson.model.entity;

import com.ascorp.prepai.lesson.lesson.model.LessonSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A lesson a student received (spec 5.1). It refers to the student by id only, never by `account`'s User. */
@Entity
@Table(name = "lessons")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Lesson {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID userId;

	@Column(nullable = false)
	private String subject;

	private String title;

	private String topic;

	private String exam;

	private String difficulty;

	private String inputText;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String responseJson;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private LessonSource source;

	private String llmProvider;

	private String llmModel;

	@Column(nullable = false)
	private boolean retried;

	private String retryReason;

	@Column(nullable = false)
	private short validationAttempts;

	private Integer generationMs;

	private Integer durationSeconds;

	private Short rating;

	private String feedback;

	@Column(nullable = false)
	private Instant createdAt;

	public void rate(short stars, String comment) {
		this.rating = stars;
		this.feedback = comment;
	}
}
