package com.ascorp.prepai.generation.rag.model;

/** A problem as the RAG cache sees it: normalised text, its numeric signature and its embedding (spec 6.3). */
public record ProblemProbe(String text, String signature, float[] vector) {
}
