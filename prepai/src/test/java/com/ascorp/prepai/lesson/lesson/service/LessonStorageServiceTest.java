package com.ascorp.prepai.lesson.lesson.service;

import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.llm.model.GenerationMetadata;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import com.ascorp.prepai.lesson.lesson.mapper.LessonMapper;
import com.ascorp.prepai.lesson.lesson.model.LessonSource;
import com.ascorp.prepai.lesson.lesson.model.ProducedLesson;
import com.ascorp.prepai.lesson.lesson.model.entity.Lesson;
import com.ascorp.prepai.lesson.lesson.repository.LessonRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonStorageServiceTest {

	private static final UUID NEW_ID = UUID.fromString("7d8e9f00-1a2b-4c3d-8e4f-5a6b7c8d9e0f");
	private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");

	@Mock
	private LessonRepository lessons;

	private final LessonMapper mapper = new LessonMapper();
	private LessonStorageService storage;

	@BeforeEach
	void setUp() {
		storage = new LessonStorageService(lessons, mapper, () -> NEW_ID, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void aGeneratedLessonIsStoredUnderANewIdWithItsGenerationDetails() {
		LessonRequest request = LessonFixtures.problem();
		GenerationMetadata metadata = new GenerationMetadata("claude", "sonnet", true, RetryReason.TIMEOUT, 2, 4200);

		LessonResponse served = storage.store(USER_ID, request,
				new ProducedLesson(LessonFixtures.lesson(), LessonSource.LLM, metadata));

		ArgumentCaptor<Lesson> saved = ArgumentCaptor.forClass(Lesson.class);
		verify(lessons).save(saved.capture());
		assertThat(served.lessonId()).isEqualTo(NEW_ID);
		assertThat(saved.getValue())
				.extracting(Lesson::getId, Lesson::getUserId, Lesson::getSubject, Lesson::getSource,
						Lesson::getLlmModel, Lesson::isRetried, Lesson::getRetryReason, Lesson::getValidationAttempts,
						Lesson::getGenerationMs, Lesson::getCreatedAt)
				.containsExactly(NEW_ID, USER_ID, "PHYSICS", LessonSource.LLM, "sonnet", true, "TIMEOUT", (short) 2,
						4200, NOW);
		assertThat(mapper.toResponse(saved.getValue()).lessonId()).isEqualTo(NEW_ID);
	}

	@Test
	void aCachedLessonHasNoGenerationDetails() {
		storage.store(USER_ID, LessonFixtures.problem(),
				ProducedLesson.cached(LessonFixtures.lesson(), LessonSource.REDIS_CACHE));

		ArgumentCaptor<Lesson> saved = ArgumentCaptor.forClass(Lesson.class);
		verify(lessons).save(saved.capture());
		assertThat(saved.getValue().getSource()).isEqualTo(LessonSource.REDIS_CACHE);
		assertThat(saved.getValue().getLlmProvider()).isNull();
		assertThat(saved.getValue().getValidationAttempts()).isEqualTo((short) 1);
	}
}
