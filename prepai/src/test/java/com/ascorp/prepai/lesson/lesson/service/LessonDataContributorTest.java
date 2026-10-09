package com.ascorp.prepai.lesson.lesson.service;

import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.lesson.lesson.mapper.LessonMapper;
import com.ascorp.prepai.lesson.lesson.model.LessonSource;
import com.ascorp.prepai.lesson.lesson.model.entity.Lesson;
import com.ascorp.prepai.lesson.lesson.repository.LessonRepository;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonDataContributorTest {

	@Mock
	private LessonRepository lessons;

	private final LessonMapper mapper = new LessonMapper();

	@Test
	void lessonsAreExportedOldestFirstUnderTheirOwnSection() {
		Lesson row = Lesson.builder().id(LessonFixtures.LESSON_ID).userId(USER_ID).subject("PHYSICS")
				.responseJson(mapper.toJson(LessonFixtures.lesson())).source(LessonSource.LLM)
				.inputText("A ball").createdAt(Instant.parse("2026-10-09T10:00:00Z")).build();
		when(lessons.findByUserIdOrderByCreatedAtAsc(USER_ID)).thenReturn(List.of(row));
		LessonDataContributor contributor = new LessonDataContributor(lessons, mapper);

		assertThat(contributor.section()).isEqualTo("lessons");
		assertThat((List<?>) contributor.export(USER_ID)).hasSize(1);
	}

	@Test
	void eraseDeletesTheStudentsLessons() {
		new LessonDataContributor(lessons, mapper).erase(USER_ID);

		verify(lessons).deleteByUserId(USER_ID);
	}
}
