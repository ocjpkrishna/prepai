package com.ascorp.prepai.generation.rag.service;

import com.ascorp.prepai.common.model.enums.Exam;
import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.rag.model.ProblemProbe;
import com.ascorp.prepai.generation.rag.model.RagLookupResult;
import com.ascorp.prepai.generation.rag.model.RagProperties;
import com.ascorp.prepai.generation.rag.model.entity.ProblemEmbedding;
import com.ascorp.prepai.generation.rag.repository.ProblemEmbeddingRepository;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

/**
 * The RAG cache (spec 6.3). A verified solution is served only when the problem is similar and its numbers are
 * identical; a miss is stored unverified so the nightly verifier can check it later. Served lessons keep the lesson id
 * they were first given, so the caller must assign a new one before saving the student's lesson.
 */
@Service
@RequiredArgsConstructor
public class RagService {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final ProblemProbeService probes;
	private final ProblemEmbeddingRepository embeddings;
	private final RagProperties properties;
	private final RagMetrics metrics;

	/** Returns a verified solution for the same problem with the same numbers, or empty when generation is needed. */
	public Optional<LessonResponse> lookup(Subject subject, String problemText) {
		Optional<ProblemProbe> probe = probes.probe(problemText);
		List<ProblemEmbedding> near = probe.map(found -> nearest(subject, found, true)).orElse(List.of());
		Optional<ProblemEmbedding> same = probe.flatMap(found -> withSameNumbers(near, found));
		metrics.recordLookup(resultOf(same, near));
		return same.map(RagService::readSolution);
	}

	/** Stores a validated lesson as unverified, or counts one more ask of a problem that is already stored. */
	@Transactional
	public void store(Subject subject, Exam exam, String problemText, LessonResponse lesson) {
		probes.probe(problemText).ifPresent(found -> saveOrCountAsk(subject, exam, found, lesson));
	}

	private void saveOrCountAsk(Subject subject, Exam exam, ProblemProbe probe, LessonResponse lesson) {
		Optional<ProblemEmbedding> same = withSameNumbers(nearest(subject, probe, false), probe);
		if (same.isPresent()) {
			embeddings.incrementAskCount(same.get().getId());
		} else {
			embeddings.save(unverified(subject, exam, probe, lesson));
		}
	}

	private List<ProblemEmbedding> nearest(Subject subject, ProblemProbe probe, boolean verifiedOnly) {
		return embeddings.findNearest(subject.name(), probe.vector(), properties.minSimilarity(), verifiedOnly);
	}

	private static Optional<ProblemEmbedding> withSameNumbers(List<ProblemEmbedding> near, ProblemProbe probe) {
		return near.stream().filter(row -> probe.signature().equals(row.getNumericSignature())).findFirst();
	}

	private static RagLookupResult resultOf(Optional<ProblemEmbedding> same, List<ProblemEmbedding> near) {
		if (same.isPresent()) {
			return RagLookupResult.HIT;
		}
		return near.isEmpty() ? RagLookupResult.MISS : RagLookupResult.REJECTED_NUMERIC;
	}

	private static ProblemEmbedding unverified(Subject subject, Exam exam, ProblemProbe probe, LessonResponse lesson) {
		return ProblemEmbedding.builder()
				.subject(subject.name())
				.exam(exam.name())
				.topic(lesson.topic())
				.problemText(probe.text())
				.solutionJson(JSON.writeValueAsString(lesson))
				.embedding(probe.vector())
				.numericSignature(probe.signature())
				.verified(false)
				.askCount(1)
				.build();
	}

	private static LessonResponse readSolution(ProblemEmbedding row) {
		return JSON.readValue(row.getSolutionJson(), LessonResponse.class);
	}
}
