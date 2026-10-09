package com.ascorp.prepai.generation.embedding.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.generation.rag.model.entity.ProblemEmbedding;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

	@Mock
	private EmbeddingModel model;

	@Test
	void embedReturnsTheVectorOfTheLocalModel() {
		float[] vector = new float[ProblemEmbedding.DIMENSIONS];
		vector[0] = 1;
		when(model.embed("a block slides")).thenReturn(vector);

		assertThat(new EmbeddingService(model).embed("a block slides")).isSameAs(vector);
	}
}
