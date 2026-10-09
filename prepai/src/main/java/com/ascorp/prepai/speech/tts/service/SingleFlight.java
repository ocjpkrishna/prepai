package com.ascorp.prepai.speech.tts.service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

/**
 * Single flight (spec 4.5): while one caller runs the work for a key, callers that ask for the same key wait for
 * its result instead of starting their own. A failure is shared the same way and is not remembered afterwards.
 */
public final class SingleFlight<V> {

	private final ConcurrentMap<String, CompletableFuture<V>> inFlight = new ConcurrentHashMap<>();

	public V run(String key, Supplier<V> work) {
		CompletableFuture<V> own = new CompletableFuture<>();
		CompletableFuture<V> shared = inFlight.putIfAbsent(key, own);
		if (shared != null) {
			return join(shared);
		}
		own.completeAsync(work, Runnable::run);
		inFlight.remove(key, own);
		return join(own);
	}

	private static <V> V join(CompletableFuture<V> future) {
		try {
			return future.join();
		} catch (CompletionException e) {
			if (e.getCause() instanceof RuntimeException failure) {
				throw failure;
			}
			throw e;
		}
	}
}
