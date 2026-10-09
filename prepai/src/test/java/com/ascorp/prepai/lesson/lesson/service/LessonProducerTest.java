package com.ascorp.prepai.lesson.lesson.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.cache.service.LessonCacheService;
import com.ascorp.prepai.generation.llm.model.GenerationMetadata;
import com.ascorp.prepai.generation.llm.model.LessonGenerationResult;
import com.ascorp.prepai.generation.llm.service.LessonGenerationService;
import com.ascorp.prepai.generation.rag.service.RagService;
import com.ascorp.prepai.lesson.lesson.model.LessonSource;
import com.ascorp.prepai.lesson.lesson.model.ProducedLesson;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonProducerTest {

	@Mock
	private LessonCacheService exactCache;

	@Mock
	private RagService rag;

	@Mock
	private LessonGenerationService generation;

	@InjectMocks
	private LessonProducer producer;

	private final LessonRequest request = LessonFixtures.problem();
	private final LessonResponse lesson = LessonFixtures.lesson();

	@Test
	void anExactCacheHitSkipsRagAndTheModel() {
		when(exactCache.find(request)).thenReturn(Optional.of(lesson));

		ProducedLesson produced = producer.produce(request);

		assertThat(produced.source()).isEqualTo(LessonSource.REDIS_CACHE);
		assertThat(produced.metadata()).isNull();
		verifyNoInteractions(rag, generation);
	}

	@Test
	void aRagHitSkipsTheModel() {
		when(exactCache.find(request)).thenReturn(Optional.empty());
		when(rag.lookup(request.subject(), request.input().text())).thenReturn(Optional.of(lesson));

		assertThat(producer.produce(request).source()).isEqualTo(LessonSource.RAG_CACHE);
		verifyNoInteractions(generation);
	}

	@Test
	void aMissGeneratesAndRemembersTheLessonInBothCaches() {
		GenerationMetadata metadata = new GenerationMetadata("fake", "fake-1", false, null, 1, 5);
		when(exactCache.find(request)).thenReturn(Optional.empty());
		when(rag.lookup(request.subject(), request.input().text())).thenReturn(Optional.empty());
		when(generation.generate(request)).thenReturn(new LessonGenerationResult(lesson, metadata));

		ProducedLesson produced = producer.produce(request);

		assertThat(produced.source()).isEqualTo(LessonSource.LLM);
		assertThat(produced.metadata()).isSameAs(metadata);
		verify(exactCache).put(request, lesson);
		verify(rag).store(request.subject(), request.exam(), request.input().text(), lesson);
	}

	@Test
	void aTopicLessonNeverUsesTheProblemCache() {
		LessonRequest topic = LessonFixtures.request(LessonRequest.Type.TOPIC, "Kinematics", null);
		when(exactCache.find(topic)).thenReturn(Optional.empty());
		when(generation.generate(topic))
				.thenReturn(new LessonGenerationResult(lesson, new GenerationMetadata("fake", "m", false, null, 1, 1)));

		producer.produce(topic);

		verify(rag, never()).lookup(topic.subject(), "Kinematics");
		verify(rag, never()).store(topic.subject(), topic.exam(), "Kinematics", lesson);
	}
}
