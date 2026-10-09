package com.ascorp.prepai.generation.embedding.service;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Service;

/** Turns problem text into a 384-dimension vector with the local all-MiniLM-L6-v2 model (spec 6.3, no network call). */
@Service
@RequiredArgsConstructor
public class EmbeddingService {

	private final EmbeddingModel model;

	public float[] embed(String text) {
		return model.embed(text);
	}
}
