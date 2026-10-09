package com.ascorp.prepai.generation.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.generation.rag.model.StoredProblem;
import com.ascorp.prepai.generation.rag.model.entity.ProblemEmbedding;
import com.ascorp.prepai.generation.rag.repository.ProblemEmbeddingRepository;
import com.ascorp.prepai.generation.validation.ValidLessons;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class ProblemReviewServiceTest {

	private static final UUID ID = UUID.randomUUID();
	private static final Instant NOW = Instant.parse("2026-10-09T02:30:00Z");

	private final ProblemEmbeddingRepository embeddings = mock(ProblemEmbeddingRepository.class);
	private final ProblemReviewService service = new ProblemReviewService(embeddings,
			Clock.fixed(NOW, ZoneOffset.UTC));

	@Test
	void pendingRowsAreReadWithTheirSolutionAndIdentity() {
		ProblemEmbedding row = ProblemEmbedding.builder()
				.subject("PHYSICS")
				.exam("JEE_MAIN")
				.problemText("A block slides")
				.solutionJson(ValidLessons.validJson())
				.build();
		ReflectionTestUtils.setField(row, "id", ID);
		when(embeddings.findUnverifiedMostAskedFirst(50)).thenReturn(List.of(row));

		List<StoredProblem> pending = service.pending(50);

		assertThat(pending).hasSize(1);
		assertThat(pending.get(0).id()).isEqualTo(ID);
		assertThat(pending.get(0).subject()).isEqualTo("PHYSICS");
		assertThat(pending.get(0).lesson()).isEqualTo(ValidLessons.valid());
	}

	@Test
	void markVerifiedStampsTheClockTime() {
		service.markVerified(ID);

		verify(embeddings).markVerified(ID, NOW);
	}

	@Test
	void replaceSolutionStoresTheCorrectionAndStampsTheClockTime() {
		service.replaceSolution(ID, ValidLessons.valid());

		verify(embeddings).replaceSolution(eq(ID), anyString(), eq(NOW));
	}

	@Test
	void backlogCountsRowsAskedAtLeastThreeTimes() {
		when(embeddings.countUnverifiedAskedAtLeast(3)).thenReturn(7L);

		assertThat(service.backlog()).isEqualTo(7L);
	}
}
