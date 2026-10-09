package com.ascorp.prepai.generation.rag.model.entity;

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
import org.hibernate.annotations.Array;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * A problem and its solution, kept for the RAG cache (spec 5.1, 6.3). It holds no user identifier (spec 2.7).
 * Only rows with `verified = true` are ever served; the rest wait for the nightly verifier.
 */
@Entity
@Table(name = "problem_embeddings")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProblemEmbedding {

	/** The local embedding model's size (spec 6.3); changing the model means re-embedding every row. */
	public static final int DIMENSIONS = 384;

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	/** The enum name of `Subject` and `Exam` (spec 5.1 VARCHAR columns). */
	@Column(nullable = false)
	private String subject;

	private String exam;

	private String topic;

	@Column(nullable = false)
	private String problemText;

	@JdbcTypeCode(SqlTypes.JSON)
	@Column(nullable = false)
	private String solutionJson;

	@JdbcTypeCode(SqlTypes.VECTOR)
	@Array(length = DIMENSIONS)
	private float[] embedding;

	private String numericSignature;

	@Column(nullable = false)
	private boolean verified;

	private Instant verifiedAt;

	@Column(nullable = false)
	private int askCount;

	/** Set by the database default (NOW()), so the app never writes it. */
	@Column(nullable = false, insertable = false, updatable = false)
	private Instant createdAt;
}
