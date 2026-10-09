package com.ascorp.prepai.generation.rag.service;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.rag.model.StoredProblem;
import com.ascorp.prepai.generation.rag.model.entity.ProblemEmbedding;
import com.ascorp.prepai.generation.rag.repository.ProblemEmbeddingRepository;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * What the nightly verifier needs from the RAG cache (spec 8.1): the batch to check, promotion to verified, and the
 * backlog gauge. Only the verifier sets `verified`; lookups never do.
 */
@Service
@RequiredArgsConstructor
public class ProblemReviewService {

	/** Rows asked at least this often count towards the backlog (spec 6.3 cold start). */
	private static final int BACKLOG_MIN_ASK_COUNT = 3;
	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final ProblemEmbeddingRepository embeddings;
	private final Clock clock;

	/** The unverified rows to check next, most asked first. */
	public List<StoredProblem> pending(int limit) {
		return embeddings.findUnverifiedMostAskedFirst(limit).stream().map(ProblemReviewService::toStored).toList();
	}

	@Transactional
	public void markVerified(UUID id) {
		embeddings.markVerified(id, clock.instant());
	}

	/** Replaces the stored solution with a corrected one and marks the row verified, as spec 8.1 step 5d asks. */
	@Transactional
	public void replaceSolution(UUID id, LessonResponse corrected) {
		embeddings.replaceSolution(id, JSON.writeValueAsString(corrected), clock.instant());
	}

	/** Unverified rows asked at least three times: the `prepai_rag_unverified_backlog` gauge. */
	public long backlog() {
		return embeddings.countUnverifiedAskedAtLeast(BACKLOG_MIN_ASK_COUNT);
	}

	private static StoredProblem toStored(ProblemEmbedding row) {
		LessonResponse lesson = JSON.readValue(row.getSolutionJson(), LessonResponse.class);
		return new StoredProblem(row.getId(), row.getSubject(), row.getExam(), row.getProblemText(), lesson);
	}
}
