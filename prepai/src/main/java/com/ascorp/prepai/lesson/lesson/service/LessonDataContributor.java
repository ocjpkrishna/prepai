package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.model.PersonalDataContributor;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.lesson.lesson.mapper.LessonMapper;
import com.ascorp.prepai.lesson.lesson.repository.LessonRepository;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Puts the student's lessons in the data download and erases them on purge (spec 2.7). */
@Component
@RequiredArgsConstructor
public class LessonDataContributor implements PersonalDataContributor {

	private static final String SECTION = "lessons";

	private final LessonRepository lessons;
	private final LessonMapper mapper;

	@Override
	public String section() {
		return SECTION;
	}

	@Override
	@Transactional(readOnly = true)
	public Object export(UUID userId) {
		return lessons.findByUserIdOrderByCreatedAtAsc(userId).stream()
				.map(lesson -> new LessonExport(lesson.getInputText(), lesson.getRating(), lesson.getFeedback(),
						lesson.getCreatedAt(), mapper.toResponse(lesson)))
				.toList();
	}

	@Override
	@Transactional
	public void erase(UUID userId) {
		lessons.deleteByUserId(userId);
	}

	private record LessonExport(String question, Short rating, String feedback, Instant createdAt,
			LessonResponse lesson) {
	}
}
