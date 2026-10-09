package com.ascorp.prepai.generation.quality.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.generation.quality.model.entity.QualityCorrection;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

/**
 * Checks the V6 mapping (`quality_corrections`) against PostgreSQL database `prepai_test` under `ddl-auto: validate`.
 * Tagged `db`: it runs on the VPS, not in cloud sessions (BUILD-DECISIONS.md, decision 4).
 */
@Tag("db")
@DataJpaTest(properties = {
		"spring.datasource.url=${DB_TEST_URL:jdbc:postgresql://localhost:5432/prepai_test}",
		"spring.flyway.out-of-order=true" // prepai_test already has V7 to V9 from earlier rows (BUILD-DECISIONS.md)
})
@AutoConfigureTestDatabase(replace = Replace.NONE)
class QualityCorrectionRepositoryTest {

	@Autowired
	private QualityCorrectionRepository corrections;

	@Autowired
	private EntityManager entityManager;

	@Test
	void aCorrectionIsStoredWithItsDatabaseTimestamp() {
		QualityCorrection saved = corrections.saveAndFlush(QualityCorrection.builder()
				.problemText("A block slides with 20 m/s")
				.originalAnswer("{\"wrong\":true}")
				.correctedAnswer("{\"wrong\":false}")
				.errorType("SIGN")
				.verifierModel("claude-fable-5-1")
				.build());

		entityManager.clear();

		assertThat(corrections.findById(saved.getId())).hasValueSatisfying(entry -> {
			assertThat(entry.getErrorType()).isEqualTo("SIGN");
			assertThat(entry.getCreatedAt()).isNotNull();
		});
	}
}
