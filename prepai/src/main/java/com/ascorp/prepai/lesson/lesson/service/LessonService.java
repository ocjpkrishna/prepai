package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.account.privacy.service.AccountGateService;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.lesson.lesson.model.ProducedLesson;
import com.ascorp.prepai.quota.ratelimit.model.SessionReservation;
import com.ascorp.prepai.quota.ratelimit.service.RateLimiterService;
import com.ascorp.prepai.quota.usage.service.UsageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Ties the modules together into one lesson request (spec 2.6, 4.2). */
@Service
@RequiredArgsConstructor
public class LessonService {

	private final AccountGateService accountGate;
	private final RateLimiterService rateLimiter;
	private final LessonInputResolver inputs;
	private final LessonProducer producer;
	private final LessonStorageService storage;
	private final UsageService usage;
	private final LessonMetrics metrics;
	private final AudioWarmup audioWarmup;

	/** A session is reserved first and counted only once the lesson is stored; any failure releases it. */
	public LessonResponse generateLesson(UUID userId, LessonRequest request) {
		accountGate.requireLessonAccess(userId);
		try (SessionReservation session = rateLimiter.reserveSession(userId)) {
			LessonRequest resolved = inputs.resolve(userId, request);
			ProducedLesson produced = producer.produce(resolved);
			LessonResponse stored = storage.store(userId, resolved, produced);
			usage.recordSession(userId, stored.lessonId(), stored.estimatedDurationSeconds());
			session.commit();
			afterCommit(resolved, produced, stored);
			return stored;
		}
	}

	private void afterCommit(LessonRequest request, ProducedLesson produced, LessonResponse stored) {
		metrics.recordGenerated(produced.source(), request);
		audioWarmup.warmUp(stored);
	}
}
