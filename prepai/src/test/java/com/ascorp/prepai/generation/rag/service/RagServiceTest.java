package com.ascorp.prepai.generation.rag.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.model.enums.Exam;
import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.embedding.service.EmbeddingService;
import com.ascorp.prepai.generation.rag.model.RagProperties;
import com.ascorp.prepai.generation.rag.model.entity.ProblemEmbedding;
import com.ascorp.prepai.generation.rag.repository.ProblemEmbeddingRepository;
import com.ascorp.prepai.generation.validation.ValidLessons;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

@ExtendWith(MockitoExtension.class)
class RagServiceTest {

	private static final UUID EXISTING = UUID.fromString("0f8fad5b-d9cb-469f-a165-000000000002");
	private static final String PROBLEM = "A block slides with 20 m/s on a rough plane";
	private static final String SAME_NUMBERS = "a block slides at 20 m/s";
	private static final String DIFFERENT_NUMBERS = "a block slides with 25 m/s on a rough plane";
	private static final String TOO_MANY_NUMBERS = "1 ".repeat(300);
	private static final String SUBJECT = Subject.PHYSICS.name();
	private static final String SIGNATURE_20 = "20m/s";
	private static final String RESULT_METRIC = "prepai.rag.lookup";
	private static final double MIN_SIMILARITY = 0.95;
	private static final JsonMapper JSON = JsonMapper.builder().build();

	@Mock
	private ProblemEmbeddingRepository embeddings;

	@Mock
	private EmbeddingService embedder;

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private final float[] vector = new float[ProblemEmbedding.DIMENSIONS];
	private final LessonResponse lesson = ValidLessons.valid();
	private RagService rag;

	@BeforeEach
	void setUp() {
		ProblemProbeService probes = new ProblemProbeService(new TextNormalizer(), new NumericSignatureService(),
				embedder);
		rag = new RagService(probes, embeddings, new RagProperties(MIN_SIMILARITY), new RagMetrics(registry));
	}

	@Test
	void lookupServesAVerifiedSolutionWithTheSameNumbers() {
		when(embedder.embed(anyString())).thenReturn(vector);
		when(embeddings.findNearest(eq(SUBJECT), any(), eq(MIN_SIMILARITY), eq(true)))
				.thenReturn(List.of(row(SIGNATURE_20, JSON.writeValueAsString(lesson))));

		assertThat(rag.lookup(Subject.PHYSICS, SAME_NUMBERS)).contains(lesson);
	}

	@Test
	void lookupRejectsTheSameWordingWithDifferentNumbers() {
		when(embedder.embed(anyString())).thenReturn(vector);
		when(embeddings.findNearest(eq(SUBJECT), any(), eq(MIN_SIMILARITY), eq(true)))
				.thenReturn(List.of(row("25m/s", JSON.writeValueAsString(lesson))));

		assertThat(rag.lookup(Subject.PHYSICS, PROBLEM)).isEmpty();
	}

	@Test
	void lookupCountsANearMissWithDifferentNumbersAsRejectedNumeric() {
		when(embedder.embed(anyString())).thenReturn(vector);
		when(embeddings.findNearest(eq(SUBJECT), any(), eq(MIN_SIMILARITY), eq(true)))
				.thenReturn(List.of(row(SIGNATURE_20, JSON.writeValueAsString(lesson))));

		rag.lookup(Subject.PHYSICS, DIFFERENT_NUMBERS);

		assertThat(registry.counter(RESULT_METRIC, "result", "rejected_numeric").count()).isEqualTo(1);
	}

	@Test
	void lookupIsAMissWhenNothingIsSimilarEnough() {
		when(embedder.embed(anyString())).thenReturn(vector);
		when(embeddings.findNearest(eq(SUBJECT), any(), eq(MIN_SIMILARITY), eq(true))).thenReturn(List.of());

		assertThat(rag.lookup(Subject.PHYSICS, PROBLEM)).isEmpty();
		assertThat(registry.counter(RESULT_METRIC, "result", "miss").count()).isEqualTo(1);
	}

	@Test
	void storeSavesAnUnverifiedRowWhenTheProblemIsNew() {
		when(embedder.embed(anyString())).thenReturn(vector);
		when(embeddings.findNearest(eq(SUBJECT), any(), eq(MIN_SIMILARITY), eq(false))).thenReturn(List.of());

		rag.store(Subject.PHYSICS, Exam.JEE_MAIN, PROBLEM, lesson);

		ArgumentCaptor<ProblemEmbedding> saved = ArgumentCaptor.forClass(ProblemEmbedding.class);
		verify(embeddings).save(saved.capture());
		assertThat(saved.getValue().isVerified()).isFalse();
		assertThat(saved.getValue().getAskCount()).isEqualTo(1);
		assertThat(saved.getValue().getNumericSignature()).isEqualTo(SIGNATURE_20);
	}

	@Test
	void storeCountsOneMoreAskWhenTheSameProblemIsAlreadyStored() {
		when(embedder.embed(anyString())).thenReturn(vector);
		when(embeddings.findNearest(eq(SUBJECT), any(), eq(MIN_SIMILARITY), eq(false)))
				.thenReturn(List.of(row(SIGNATURE_20, "{}")));

		rag.store(Subject.PHYSICS, Exam.JEE_MAIN, SAME_NUMBERS, lesson);

		verify(embeddings).incrementAskCount(EXISTING);
		verify(embeddings, never()).save(any());
	}

	@Test
	void storeAndLookupSkipProblemsWhoseSignatureIsTooLongToStore() {
		rag.store(Subject.PHYSICS, Exam.JEE_MAIN, TOO_MANY_NUMBERS, lesson);

		assertThat(rag.lookup(Subject.PHYSICS, TOO_MANY_NUMBERS)).isEmpty();
		verify(embeddings, never()).save(any());
		verify(embedder, never()).embed(anyString());
	}

	private static ProblemEmbedding row(String signature, String solutionJson) {
		return ProblemEmbedding.builder()
				.id(EXISTING)
				.subject(SUBJECT)
				.problemText(PROBLEM)
				.solutionJson(solutionJson)
				.numericSignature(signature)
				.verified(true)
				.askCount(1)
				.build();
	}
}
