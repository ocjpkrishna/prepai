package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.cache.service.LessonCacheService;
import com.ascorp.prepai.generation.llm.model.LessonGenerationResult;
import com.ascorp.prepai.generation.llm.service.LessonGenerationService;
import com.ascorp.prepai.generation.rag.service.RagService;
import com.ascorp.prepai.lesson.lesson.model.LessonSource;
import com.ascorp.prepai.lesson.lesson.model.ProducedLesson;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Finds the cheapest valid lesson: the exact-match cache, then the RAG cache, then the model (spec 2.6). */
@Service
@RequiredArgsConstructor
public class LessonProducer {

	private final LessonCacheService exactCache;
	private final RagService rag;
	private final LessonGenerationService generation;

	public ProducedLesson produce(LessonRequest request) {
		return fromExactCache(request)
				.or(() -> fromRag(request))
				.orElseGet(() -> generateAndRemember(request));
	}

	private Optional<ProducedLesson> fromExactCache(LessonRequest request) {
		return exactCache.find(request).map(lesson -> ProducedLesson.cached(lesson, LessonSource.REDIS_CACHE));
	}

	private Optional<ProducedLesson> fromRag(LessonRequest request) {
		if (!isProblem(request)) {
			return Optional.empty();
		}
		return rag.lookup(request.subject(), request.input().text())
				.map(lesson -> ProducedLesson.cached(lesson, LessonSource.RAG_CACHE));
	}

	private ProducedLesson generateAndRemember(LessonRequest request) {
		LessonGenerationResult result = generation.generate(request);
		exactCache.put(request, result.lesson());
		storeForRag(request, result.lesson());
		return new ProducedLesson(result.lesson(), LessonSource.LLM, result.metadata());
	}

	private void storeForRag(LessonRequest request, LessonResponse lesson) {
		if (isProblem(request)) {
			rag.store(request.subject(), request.exam(), request.input().text(), lesson);
		}
	}

	private static boolean isProblem(LessonRequest request) {
		return request.type() == LessonRequest.Type.PROBLEM;
	}
}
