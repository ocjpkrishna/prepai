package com.ascorp.prepai.generation.cache.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.model.enums.Difficulty;
import com.ascorp.prepai.common.model.enums.Exam;
import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.cache.repository.LessonCacheRepository;
import com.ascorp.prepai.generation.validation.ValidLessons;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonCacheServiceTest {

	private static final Duration SEVEN_DAYS = Duration.ofDays(7);

	@Mock
	private LessonCacheRepository cache;

	private LessonCacheService lessons;

	@BeforeEach
	void setUp() {
		lessons = new LessonCacheService(cache);
	}

	@Test
	void findReturnsTheLessonStoredForAnIdenticalRequest() {
		LessonResponse lesson = ValidLessons.valid();
		ArgumentCaptor<String> key = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> json = ArgumentCaptor.forClass(String.class);
		lessons.put(problem("A block slides on a rough plane"), lesson);
		verify(cache).save(key.capture(), json.capture(), eq(SEVEN_DAYS));
		when(cache.find(key.getValue())).thenReturn(Optional.of(json.getValue()));

		assertThat(lessons.find(problem("A block slides on a rough plane"))).contains(lesson);
	}

	@Test
	void findIsEmptyWhenTheRequestWasNeverCached() {
		when(cache.find(anyString())).thenReturn(Optional.empty());

		assertThat(lessons.find(problem("A block slides on a rough plane"))).isEmpty();
	}

	@Test
	void differentRequestsGetDifferentKeys() {
		ArgumentCaptor<String> keys = ArgumentCaptor.forClass(String.class);
		lessons.put(problem("A block slides with 20 m/s"), ValidLessons.valid());
		lessons.put(problem("A block slides with 25 m/s"), ValidLessons.valid());
		verify(cache, times(2)).save(keys.capture(), anyString(), eq(SEVEN_DAYS));

		assertThat(keys.getAllValues())
				.hasSize(2)
				.allMatch(key -> key.startsWith("lesson:cache:"))
				.doesNotHaveDuplicates();
	}

	@Test
	void evictRemovesTheKeyOfTheSameRequest() {
		ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
		ArgumentCaptor<String> removed = ArgumentCaptor.forClass(String.class);
		lessons.put(problem("A block slides on a rough plane"), ValidLessons.valid());
		lessons.evict(problem("A block slides on a rough plane"));
		verify(cache).save(stored.capture(), anyString(), eq(SEVEN_DAYS));
		verify(cache).delete(removed.capture());

		assertThat(removed.getValue()).isEqualTo(stored.getValue());
	}

	private static LessonRequest problem(String text) {
		return new LessonRequest(LessonRequest.Type.PROBLEM, Subject.PHYSICS, Exam.JEE_MAIN,
				new LessonRequest.Input(text, null), Difficulty.MEDIUM, Language.EN);
	}
}
