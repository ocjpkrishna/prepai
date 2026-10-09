package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.lesson.lesson.mapper.LessonMapper;
import com.ascorp.prepai.lesson.lesson.model.dto.FeedbackRequest;
import com.ascorp.prepai.lesson.lesson.model.dto.LessonHistoryPage;
import com.ascorp.prepai.lesson.lesson.model.entity.Lesson;
import com.ascorp.prepai.lesson.lesson.repository.LessonRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** A student's own lessons: fetch one, browse the history, rate one (spec 4.2). */
@Service
@RequiredArgsConstructor
public class LessonHistoryService {

	static final int MAX_PAGE_SIZE = 50;

	private final LessonRepository lessons;
	private final LessonMapper mapper;

	@Transactional(readOnly = true)
	public LessonResponse get(UUID userId, UUID lessonId) {
		return mapper.toResponse(find(userId, lessonId));
	}

	@Transactional(readOnly = true)
	public LessonHistoryPage history(UUID userId, Subject subject, int page, int size) {
		PageRequest paging = PageRequest.of(Math.max(page, 0), Math.clamp(size, 1, MAX_PAGE_SIZE),
				Sort.by(Sort.Direction.DESC, "createdAt"));
		Page<Lesson> found = subject == null
				? lessons.findByUserId(userId, paging)
				: lessons.findByUserIdAndSubject(userId, subject.name(), paging);
		return new LessonHistoryPage(found.map(mapper::toSummary).getContent(), found.getNumber(), found.getSize(),
				found.getTotalElements(), found.getTotalPages());
	}

	@Transactional
	public void rate(UUID userId, UUID lessonId, FeedbackRequest feedback) {
		find(userId, lessonId).rate((short) feedback.rating(), feedback.comment());
	}

	Lesson find(UUID userId, UUID lessonId) {
		return lessons.findByIdAndUserId(lessonId, userId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "We couldn't find this lesson."));
	}
}
