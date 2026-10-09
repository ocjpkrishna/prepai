package com.ascorp.prepai.generation.quality.service;

import com.ascorp.prepai.generation.rag.service.ProblemReviewService;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

/** The unverified backlog gauge of spec 8.4, exported as `prepai_rag_unverified_backlog`. */
@Component
public class QualityMetrics {

	private static final String BACKLOG = "prepai.rag.unverified.backlog";

	public QualityMetrics(MeterRegistry registry, ProblemReviewService reviews) {
		Gauge.builder(BACKLOG, reviews, ProblemReviewService::backlog).register(registry);
	}
}
