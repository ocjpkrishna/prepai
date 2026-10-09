package com.ascorp.prepai.lesson.lesson.service;

import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.LESSON_ID;
import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.lesson.lesson.mapper.LessonMapper;
import com.ascorp.prepai.lesson.lesson.model.LessonSource;
import com.ascorp.prepai.lesson.lesson.model.dto.FeedbackRequest;
import com.ascorp.prepai.lesson.lesson.model.dto.LessonHistoryPage;
import com.ascorp.prepai.lesson.lesson.model.entity.Lesson;
import com.ascorp.prepai.lesson.lesson.repository.LessonRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class LessonHistoryServiceTest {

	@Mock
	private LessonRepository lessons;

	private final LessonMapper mapper = new LessonMapper();
	private LessonHistoryService history;
	private Lesson row;

	@BeforeEach
	void setUp() {
		history = new LessonHistoryService(lessons, mapper);
		row = Lesson.builder().id(LESSON_ID).userId(USER_ID).subject("PHYSICS").title("Projectile Motion")
				.topic("Kinematics").exam("JEE_MAIN").difficulty("MEDIUM")
				.responseJson(mapper.toJson(LessonFixtures.lesson())).source(LessonSource.LLM)
				.createdAt(Instant.parse("2026-10-09T10:00:00Z")).build();
	}

	@Test
	void aStudentGetsTheirOwnStoredLesson() {
		when(lessons.findByIdAndUserId(LESSON_ID, USER_ID)).thenReturn(Optional.of(row));

		assertThat(history.get(USER_ID, LESSON_ID).title()).isEqualTo("Projectile Motion");
	}

	@Test
	void someoneElsesLessonIsNotFound() {
		when(lessons.findByIdAndUserId(LESSON_ID, USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> history.get(USER_ID, LESSON_ID)).isInstanceOf(ApiException.class)
				.extracting(failure -> ((ApiException) failure).getCode()).isEqualTo(ErrorCode.NOT_FOUND);
	}

	@Test
	void historyIsNewestFirstAndFiltersBySubject() {
		when(lessons.findByUserIdAndSubject(any(), any(), any())).thenReturn(new PageImpl<>(List.of(row)));

		LessonHistoryPage page = history.history(USER_ID, Subject.PHYSICS, 0, 20);

		assertThat(page.items()).singleElement().extracting("lessonId", "title").containsExactly(LESSON_ID,
				"Projectile Motion");
		ArgumentCaptor<Pageable> paging = ArgumentCaptor.forClass(Pageable.class);
		verify(lessons).findByUserIdAndSubject(any(), any(), paging.capture());
		assertThat(paging.getValue().getSort().getOrderFor("createdAt").isDescending()).isTrue();
	}

	@Test
	void anOversizedPageIsCutToTheMaximumAndNoSubjectListsEverything() {
		when(lessons.findByUserId(any(), any())).thenReturn(new PageImpl<>(List.of()));

		history.history(USER_ID, null, -3, 5000);

		ArgumentCaptor<Pageable> paging = ArgumentCaptor.forClass(Pageable.class);
		verify(lessons).findByUserId(any(), paging.capture());
		assertThat(paging.getValue().getPageSize()).isEqualTo(LessonHistoryService.MAX_PAGE_SIZE);
		assertThat(paging.getValue().getPageNumber()).isZero();
	}

	@Test
	void feedbackIsSavedOnTheLesson() {
		when(lessons.findByIdAndUserId(LESSON_ID, USER_ID)).thenReturn(Optional.of(row));

		history.rate(USER_ID, LESSON_ID, new FeedbackRequest(4, "Clear"));

		assertThat(row.getRating()).isEqualTo((short) 4);
		assertThat(row.getFeedback()).isEqualTo("Clear");
	}
}
