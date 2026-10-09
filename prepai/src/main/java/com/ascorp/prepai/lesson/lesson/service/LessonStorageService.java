package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.llm.model.GenerationMetadata;
import com.ascorp.prepai.lesson.lesson.mapper.LessonMapper;
import com.ascorp.prepai.lesson.lesson.model.ProducedLesson;
import com.ascorp.prepai.lesson.lesson.model.entity.Lesson;
import com.ascorp.prepai.lesson.lesson.repository.LessonRepository;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Stores a produced lesson for one student under a new lesson id (spec 5.1). */
@Service
@RequiredArgsConstructor
public class LessonStorageService {

	private final LessonRepository lessons;
	private final LessonMapper mapper;
	private final Supplier<UUID> ids;
	private final Clock clock;

	@Transactional
	public LessonResponse store(UUID userId, LessonRequest request, ProducedLesson produced) {
		LessonResponse lesson = produced.lesson().withLessonId(ids.get());
		lessons.save(row(userId, request, produced, lesson));
		return lesson;
	}

	private Lesson row(UUID userId, LessonRequest request, ProducedLesson produced, LessonResponse lesson) {
		Lesson.LessonBuilder row = Lesson.builder()
				.id(lesson.lessonId())
				.userId(userId)
				.subject(request.subject().name())
				.title(lesson.title())
				.topic(lesson.topic())
				.exam(request.exam().name())
				.difficulty(request.difficulty().name())
				.inputText(request.input().text())
				.responseJson(mapper.toJson(lesson))
				.source(produced.source())
				.validationAttempts((short) 1)
				.durationSeconds(lesson.estimatedDurationSeconds())
				.createdAt(clock.instant());
		return withGeneration(row, produced.metadata()).build();
	}

	private static Lesson.LessonBuilder withGeneration(Lesson.LessonBuilder row, GenerationMetadata metadata) {
		if (metadata == null) {
			return row;
		}
		return row.llmProvider(metadata.provider())
				.llmModel(metadata.model())
				.retried(metadata.retried())
				.retryReason(metadata.retryReason() == null ? null : metadata.retryReason().name())
				.validationAttempts((short) metadata.validationAttempts())
				.generationMs(Math.toIntExact(metadata.generationMs()));
	}
}
