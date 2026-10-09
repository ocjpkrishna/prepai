package com.ascorp.prepai.generation.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;

import com.ascorp.prepai.generation.embedding.service.EmbeddingService;
import com.ascorp.prepai.generation.rag.model.entity.ProblemEmbedding;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

/** The signature a student's wording produces must change whenever a number, its sign or its unit changes. */
class ProblemProbeServiceTest {

	private final EmbeddingService embedder = Mockito.mock(EmbeddingService.class);
	private final ProblemProbeService probes =
			new ProblemProbeService(new TextNormalizer(), new NumericSignatureService(), embedder);

	private String signatureOf(String problem) {
		Mockito.when(embedder.embed(anyString())).thenReturn(new float[ProblemEmbedding.DIMENSIONS]);
		return probes.probe(problem).orElseThrow().signature();
	}

	@Test
	void aTypographicMinusSignIsPartOfTheNumber() {
		assertThat(signatureOf("A body moves at −3 m/s")).isNotEqualTo(signatureOf("A body moves at 3 m/s"));
	}

	@Test
	void superscriptExponentsAreRead() {
		assertThat(signatureOf("A charge of 2 × 10⁻⁹ C"))
				.isNotEqualTo(signatureOf("A charge of 2 × 10⁻¹¹ C"));
	}

	@Test
	void squaredAndCubedUnitsDiffer() {
		assertThat(signatureOf("An area of 5 m²")).isNotEqualTo(signatureOf("An area of 5 m³"));
	}

	@Test
	void fullwidthDigitsAreRead() {
		assertThat(signatureOf("A mass of ５ kg")).isEqualTo(signatureOf("A mass of 5 kg"));
	}

	@Test
	void prefixesThatDifferOnlyInCaseDiffer() {
		assertThat(signatureOf("A heater of 5 mW")).isNotEqualTo(signatureOf("A heater of 5 MW"));
	}

	@Test
	void unitSpellingsStillMatchWhateverTheCase() {
		assertThat(signatureOf("A force of 5 Newtons")).isEqualTo(signatureOf("a force of 5 newton"));
	}
}
