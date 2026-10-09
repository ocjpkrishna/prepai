package com.ascorp.prepai.generation.rag.model;

/** The outcome of one RAG lookup, the `result` tag of `prepai_rag_lookup_total` (spec 8.4). */
public enum RagLookupResult {
	HIT("hit"),
	MISS("miss"),
	REJECTED_NUMERIC("rejected_numeric");

	private final String tag;

	RagLookupResult(String tag) {
		this.tag = tag;
	}

	public String tag() {
		return tag;
	}
}
