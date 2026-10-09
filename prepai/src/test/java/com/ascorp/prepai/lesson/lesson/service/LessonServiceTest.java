package com.ascorp.prepai.lesson.lesson.service;

import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.privacy.service.AccountGateService;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.lesson.lesson.model.LessonSource;
import com.ascorp.prepai.lesson.lesson.model.ProducedLesson;
import com.ascorp.prepai.quota.ratelimit.service.RateLimiterService;
import com.ascorp.prepai.quota.usage.service.UsageService;
import com.ascorp.prepai.quota.ratelimit.model.SessionReservation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonServiceTest {

	@Mock
	private AccountGateService accountGate;

	@Mock
	private RateLimiterService rateLimiter;

	@Mock
	private LessonInputResolver inputs;

	@Mock
	private LessonProducer producer;

	@Mock
	private LessonStorageService storage;

	@Mock
	private UsageService usage;

	@Mock
	private LessonMetrics metrics;

	@Mock
	private AudioWarmup audioWarmup;

	private LessonService service;
	private Runnable release;
	private final LessonRequest request = LessonFixtures.problem();
	private final LessonResponse lesson = LessonFixtures.lesson();
	private final ProducedLesson produced = ProducedLesson.cached(lesson, LessonSource.RAG_CACHE);

	@BeforeEach
	void setUp() {
		service = new LessonService(accountGate, rateLimiter, inputs, producer, storage, usage, metrics, audioWarmup);
		release = mock(Runnable.class);
		when(rateLimiter.reserveSession(USER_ID)).thenReturn(new SessionReservation(release));
	}

	@Test
	void aStoredLessonCountsTheSessionAndWarmsUpAudioInOrder() {
		when(inputs.resolve(USER_ID, request)).thenReturn(request);
		when(producer.produce(request)).thenReturn(produced);
		when(storage.store(USER_ID, request, produced)).thenReturn(lesson);

		LessonResponse served = service.generateLesson(USER_ID, request);

		assertThat(served).isSameAs(lesson);
		InOrder order = inOrder(accountGate, storage, usage, metrics, audioWarmup);
		order.verify(accountGate).requireLessonAccess(USER_ID);
		order.verify(storage).store(USER_ID, request, produced);
		order.verify(usage).recordSession(USER_ID, lesson.lessonId(), lesson.estimatedDurationSeconds());
		order.verify(metrics).recordGenerated(LessonSource.RAG_CACHE, request);
		order.verify(audioWarmup).warmUp(lesson);
		verify(release, never()).run();
	}

	@Test
	void aFailedGenerationReleasesTheSessionAndStoresNothing() {
		when(inputs.resolve(USER_ID, request)).thenReturn(request);
		when(producer.produce(request)).thenThrow(new ApiException(ErrorCode.LLM_UNAVAILABLE));

		assertThatThrownBy(() -> service.generateLesson(USER_ID, request))
				.isInstanceOf(ApiException.class)
				.extracting(failure -> ((ApiException) failure).getCode()).isEqualTo(ErrorCode.LLM_UNAVAILABLE);

		verify(release).run();
		verify(storage, never()).store(USER_ID, request, produced);
		verify(usage, never()).recordSession(USER_ID, lesson.lessonId(), 0);
	}

	@Test
	void aFailureWhileRecordingUsageReleasesTheSession() {
		when(inputs.resolve(USER_ID, request)).thenReturn(request);
		when(producer.produce(request)).thenReturn(produced);
		when(storage.store(USER_ID, request, produced)).thenReturn(lesson);
		doThrow(new IllegalStateException("db down")).when(usage)
				.recordSession(USER_ID, lesson.lessonId(), lesson.estimatedDurationSeconds());

		assertThatThrownBy(() -> service.generateLesson(USER_ID, request)).isInstanceOf(IllegalStateException.class);

		verify(release).run();
		verify(audioWarmup, never()).warmUp(lesson);
	}

	@Test
	void anAccountThatMayNotLearnNeverReservesASession() {
		doThrow(new ApiException(ErrorCode.EMAIL_NOT_VERIFIED)).when(accountGate).requireLessonAccess(USER_ID);

		assertThatThrownBy(() -> service.generateLesson(USER_ID, request)).isInstanceOf(ApiException.class);

		verify(rateLimiter, never()).reserveSession(USER_ID);
	}
}
