package com.ascorp.prepai.generation.rag.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.generation.rag.model.entity.ProblemEmbedding;
import java.time.Instant;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

/**
 * Runs the pgvector query and the entity mapping against PostgreSQL database `prepai_test` with the V5 schema
 * (BUILD-DECISIONS.md, decision 4). Tagged `db`: it runs on the VPS, not in cloud sessions.
 */
@Tag("db")
@DataJpaTest(properties = {
		"spring.datasource.url=${DB_TEST_URL:jdbc:postgresql://localhost:5432/prepai_test}",
		"spring.flyway.out-of-order=true" // prepai_test already has V7 to V9 from earlier rows (BUILD-DECISIONS.md)
})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ProblemEmbeddingRepositoryTest {

	private static final double MIN_SIMILARITY = 0.95;

	@Autowired
	private ProblemEmbeddingRepository embeddings;

	@Test
	void verifiedOnlyLookupSkipsUnverifiedRows() {
		embeddings.saveAndFlush(row(axis(0), true));
		embeddings.saveAndFlush(row(axis(1), false));

		assertThat(embeddings.findNearest(Subject.PHYSICS.name(), axis(1), MIN_SIMILARITY, true)).isEmpty();
	}

	@Test
	void anyStatusLookupFindsUnverifiedRows() {
		embeddings.saveAndFlush(row(axis(1), false));

		assertThat(embeddings.findNearest(Subject.PHYSICS.name(), axis(1), MIN_SIMILARITY, false)).hasSize(1);
	}

	@Test
	void rowsBelowTheSimilarityThresholdAreLeftOut() {
		embeddings.saveAndFlush(row(axis(0), true));

		assertThat(embeddings.findNearest(Subject.PHYSICS.name(), axis(1), MIN_SIMILARITY, true)).isEmpty();
	}

	@Test
	void otherSubjectsAreLeftOut() {
		embeddings.saveAndFlush(row(axis(0), true));

		assertThat(embeddings.findNearest(Subject.CHEMISTRY.name(), axis(0), MIN_SIMILARITY, true)).isEmpty();
	}

	@Test
	void askCountIncrementsByOne() {
		ProblemEmbedding saved = embeddings.saveAndFlush(row(axis(0), false));

		embeddings.incrementAskCount(saved.getId());
		embeddings.flush();

		assertThat(embeddings.findById(saved.getId())).hasValueSatisfying(
				row -> assertThat(row.getAskCount()).isEqualTo(2));
	}

	@Test
	void reviewBatchIsUnverifiedRowsMostAskedFirstAndLimited() {
		embeddings.saveAndFlush(asked(axis(0), false, 1));
		embeddings.saveAndFlush(asked(axis(1), false, 5));
		embeddings.saveAndFlush(asked(axis(2), false, 3));
		embeddings.saveAndFlush(asked(axis(3), true, 9));

		assertThat(embeddings.findUnverifiedMostAskedFirst(2)).extracting(ProblemEmbedding::getAskCount)
				.containsExactly(5, 3);
	}

	@Test
	void markVerifiedSetsTheFlagAndTheTime() {
		ProblemEmbedding saved = embeddings.saveAndFlush(asked(axis(0), false, 1));
		Instant at = Instant.parse("2026-10-09T02:30:00Z");

		embeddings.markVerified(saved.getId(), at);
		embeddings.flush();

		assertThat(embeddings.findById(saved.getId())).hasValueSatisfying(row -> {
			assertThat(row.isVerified()).isTrue();
			assertThat(row.getVerifiedAt()).isEqualTo(at);
		});
	}

	@Test
	void replaceSolutionStoresTheCorrectionAsVerified() {
		ProblemEmbedding saved = embeddings.saveAndFlush(asked(axis(0), false, 1));

		embeddings.replaceSolution(saved.getId(), "{\"corrected\":true}", Instant.parse("2026-10-09T02:30:00Z"));
		embeddings.flush();

		assertThat(embeddings.findById(saved.getId())).hasValueSatisfying(row -> {
			assertThat(row.isVerified()).isTrue();
			assertThat(row.getSolutionJson()).contains("corrected");
		});
	}

	@Test
	void backlogCountsUnverifiedRowsAskedAtLeastThreeTimes() {
		embeddings.saveAndFlush(asked(axis(0), false, 1));
		embeddings.saveAndFlush(asked(axis(1), false, 3));
		embeddings.saveAndFlush(asked(axis(2), false, 4));
		embeddings.saveAndFlush(asked(axis(3), true, 9));

		assertThat(embeddings.countUnverifiedAskedAtLeast(3)).isEqualTo(2);
	}

	private static ProblemEmbedding asked(float[] vector, boolean verified, int askCount) {
		return ProblemEmbedding.builder()
				.subject(Subject.PHYSICS.name())
				.problemText("A block slides with 20 m/s")
				.solutionJson("{}")
				.embedding(vector)
				.numericSignature("20m/s")
				.verified(verified)
				.askCount(askCount)
				.build();
	}

	private static float[] axis(int index) {
		float[] vector = new float[ProblemEmbedding.DIMENSIONS];
		vector[index] = 1;
		return vector;
	}

	private static ProblemEmbedding row(float[] vector, boolean verified) {
		return ProblemEmbedding.builder()
				.subject(Subject.PHYSICS.name())
				.problemText("A block slides with 20 m/s")
				.solutionJson("{}")
				.embedding(vector)
				.numericSignature("20m/s")
				.verified(verified)
				.askCount(1)
				.build();
	}
}
