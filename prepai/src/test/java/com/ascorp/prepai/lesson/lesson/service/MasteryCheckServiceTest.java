package com.ascorp.prepai.lesson.lesson.service;

import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.LESSON_ID;
import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.validation.ValidLessons;
import com.ascorp.prepai.lesson.lesson.model.dto.MasteryCheckResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class MasteryCheckServiceTest {

	@Mock
	private LessonHistoryService history;

	@InjectMocks
	private MasteryCheckService masteryChecks;

	@Test
	void theCorrectOptionIsMarkedRight() {
		LessonResponse lesson = LessonFixtures.lesson();
		when(history.get(USER_ID, LESSON_ID)).thenReturn(lesson);
		LessonResponse.Option right = lesson.masteryCheck().options().stream()
				.filter(LessonResponse.Option::correct).findFirst().orElseThrow();

		MasteryCheckResponse answer = masteryChecks.answer(USER_ID, LESSON_ID, right.id());

		assertThat(answer.correct()).isTrue();
		assertThat(answer.explanation()).isEqualTo(lesson.masteryCheck().explanation());
	}

	@Test
	void aWrongOptionIsMarkedWrong() {
		LessonResponse lesson = LessonFixtures.lesson();
		when(history.get(USER_ID, LESSON_ID)).thenReturn(lesson);
		LessonResponse.Option wrong = lesson.masteryCheck().options().stream()
				.filter(option -> !option.correct()).findFirst().orElseThrow();

		assertThat(masteryChecks.answer(USER_ID, LESSON_ID, wrong.id()).correct()).isFalse();
	}

	@Test
	void anUnknownOptionIsRejected() {
		when(history.get(USER_ID, LESSON_ID)).thenReturn(LessonFixtures.lesson());

		assertThatThrownBy(() -> masteryChecks.answer(USER_ID, LESSON_ID, "Z")).isInstanceOf(ApiException.class)
				.extracting(failure -> ((ApiException) failure).getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
	}

	@Test
	void aLessonWithoutAMasteryCheckIsNotFound() {
		LessonResponse base = LessonFixtures.lesson();
		LessonResponse without = ValidLessons.withParts(base, base.steps(), base.summary(), null);
		when(history.get(USER_ID, LESSON_ID)).thenReturn(without);

		assertThatThrownBy(() -> masteryChecks.answer(USER_ID, LESSON_ID, "A")).isInstanceOf(ApiException.class)
				.extracting(failure -> ((ApiException) failure).getCode()).isEqualTo(ErrorCode.NOT_FOUND);
	}
}
