package com.ascorp.prepai.generation.rag.service;

import com.ascorp.prepai.generation.rag.model.RagLookupResult;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** The RAG lookup counter of spec 8.4, exported as `prepai_rag_lookup_total`. */
@Component
@RequiredArgsConstructor
public class RagMetrics {

	private static final String LOOKUP = "prepai.rag.lookup";

	private final MeterRegistry registry;

	public void recordLookup(RagLookupResult result) {
		registry.counter(LOOKUP, "result", result.tag()).increment();
	}
}
