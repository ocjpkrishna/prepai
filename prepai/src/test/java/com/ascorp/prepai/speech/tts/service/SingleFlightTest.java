package com.ascorp.prepai.speech.tts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class SingleFlightTest {

	private static final String KEY = "same-narration";

	@Test
	void callersThatOverlapOnOneKeyShareOneRun() throws InterruptedException {
		SingleFlight<String> flight = new SingleFlight<>();
		AtomicInteger runs = new AtomicInteger();
		CountDownLatch started = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		Thread first = Thread.ofVirtual().start(() -> flight.run(KEY, () -> {
			runs.incrementAndGet();
			started.countDown();
			await(release);
			return "audio";
		}));
		started.await();

		Thread second = Thread.ofVirtual().start(() -> flight.run(KEY, () -> countRun(runs)));
		awaitWaiting(second);
		release.countDown();
		first.join();
		second.join();

		assertThat(runs).hasValue(1);
	}

	private static String countRun(AtomicInteger runs) {
		runs.incrementAndGet();
		return "other";
	}

	/** Returns once the thread is parked, so it has already found the key taken and is waiting on it. */
	private static void awaitWaiting(Thread thread) {
		while (thread.isAlive() && thread.getState() != Thread.State.WAITING) {
			Thread.onSpinWait();
		}
	}

	@Test
	void aSequentialCallRunsAgainBecauseNothingIsRemembered() {
		SingleFlight<String> flight = new SingleFlight<>();
		AtomicInteger runs = new AtomicInteger();

		flight.run(KEY, () -> "first" + runs.incrementAndGet());

		assertThat(flight.run(KEY, () -> "second" + runs.incrementAndGet())).isEqualTo("second2");
	}

	@Test
	void aFailureIsThrownToTheCallerAndNotRemembered() {
		SingleFlight<String> flight = new SingleFlight<>();

		assertThatThrownBy(() -> flight.run(KEY, () -> {
			throw new ApiException(ErrorCode.TTS_UNAVAILABLE);
		})).isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getCode()).isEqualTo(ErrorCode.TTS_UNAVAILABLE));

		assertThat(flight.run(KEY, () -> "recovered")).isEqualTo("recovered");
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await();
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
