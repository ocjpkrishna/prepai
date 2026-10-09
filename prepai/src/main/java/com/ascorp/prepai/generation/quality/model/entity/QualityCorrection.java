package com.ascorp.prepai.generation.quality.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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

/**
 * One wrong lesson the nightly verifier found, with its correction (spec 5.2, 8.1 step 5a). The lesson id is not
 * mapped yet: rows from the verifier have no lesson, and row 13 adds the link.
 */
@Entity
@Table(name = "quality_corrections")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class QualityCorrection {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Column(nullable = false)
	private String problemText;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String originalAnswer;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String correctedAnswer;

	private String errorType;

	private String verifierModel;

	/** Set by the database default (NOW()), so the app never writes it. */
	@Column(nullable = false, insertable = false, updatable = false)
	private Instant createdAt;
}
