package com.ascorp.prepai.generation.rag.repository;

import com.ascorp.prepai.generation.rag.model.entity.ProblemEmbedding;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProblemEmbeddingRepository extends JpaRepository<ProblemEmbedding, UUID> {

	/** The top three problems of a subject by cosine similarity, at least `minSimilarity` (spec 6.3, step 4). */
	default List<ProblemEmbedding> findNearest(String subject, float[] vector, double minSimilarity,
			boolean verifiedOnly) {
		return findNearestByLiteral(subject, toLiteral(vector), minSimilarity, verifiedOnly);
	}

	@Query(value = """
			SELECT * FROM problem_embeddings
			WHERE subject = :subject
			  AND (NOT :verifiedOnly OR verified)
			  AND 1 - (embedding <=> CAST(:vector AS vector)) >= :minSimilarity
			ORDER BY embedding <=> CAST(:vector AS vector)
			LIMIT 3
			""", nativeQuery = true)
	List<ProblemEmbedding> findNearestByLiteral(@Param("subject") String subject, @Param("vector") String vector,
			@Param("minSimilarity") double minSimilarity, @Param("verifiedOnly") boolean verifiedOnly);

	/** A bulk update, so the persistence context is cleared: a loaded row would otherwise keep the old count. */
	@Modifying(clearAutomatically = true)
	@Query("UPDATE ProblemEmbedding p SET p.askCount = p.askCount + 1 WHERE p.id = :id")
	void incrementAskCount(@Param("id") UUID id);

	/** The nightly verifier's batch: unverified rows, most asked first (spec 8.1, step 1). */
	@Query(value = """
			SELECT * FROM problem_embeddings
			WHERE NOT verified
			ORDER BY ask_count DESC, created_at
			LIMIT :limit
			""", nativeQuery = true)
	List<ProblemEmbedding> findUnverifiedMostAskedFirst(@Param("limit") int limit);

	@Modifying(clearAutomatically = true)
	@Query("UPDATE ProblemEmbedding p SET p.verified = true, p.verifiedAt = :at WHERE p.id = :id")
	void markVerified(@Param("id") UUID id, @Param("at") Instant at);

	@Modifying(clearAutomatically = true)
	@Query("""
			UPDATE ProblemEmbedding p
			SET p.solutionJson = :json, p.verified = true, p.verifiedAt = :at
			WHERE p.id = :id
			""")
	void replaceSolution(@Param("id") UUID id, @Param("json") String json, @Param("at") Instant at);

	@Query("SELECT COUNT(p) FROM ProblemEmbedding p WHERE NOT p.verified AND p.askCount >= :minAskCount")
	long countUnverifiedAskedAtLeast(@Param("minAskCount") int minAskCount);

	private static String toLiteral(float[] vector) {
		StringBuilder literal = new StringBuilder("[");
		for (int index = 0; index < vector.length; index++) {
			literal.append(index == 0 ? "" : ",").append(vector[index]);
		}
		return literal.append(']').toString();
	}
}
