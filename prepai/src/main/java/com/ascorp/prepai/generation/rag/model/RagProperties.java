package com.ascorp.prepai.generation.rag.model;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Settings of the RAG cache (`prepai.rag.*`, spec 10.3): how similar a problem must be to count as the same. */
@ConfigurationProperties("prepai.rag")
public record RagProperties(double minSimilarity) {
}
