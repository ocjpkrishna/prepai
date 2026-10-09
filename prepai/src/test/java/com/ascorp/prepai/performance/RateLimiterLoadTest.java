package com.ascorp.prepai.performance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.user.service.UserService;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Plan;
import com.ascorp.prepai.quota.ratelimit.repository.RateLimitRepository;
import com.ascorp.prepai.quota.ratelimit.service.BurstLimiter;
import com.ascorp.prepai.quota.ratelimit.service.RateLimiterService;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

/**
 * Many threads hit the limiters at once, with the counters behind them made atomic the way Redis scripts are.
 * Every admitted request must be counted exactly once, so the limits hold under contention.
 */
class RateLimiterLoadTest {

	private static final UUID STUDENT = UUID.fromString("5f0c1d2e-3a4b-4c5d-8e6f-7a8b9c0d1e2f");
	private static final Clock CLOCK = Clock.fixed(Instant.parse("2026-10-09T06:00:00Z"), ZoneOffset.UTC);
	private static final int THREADS = 16;
	private static final int ATTEMPTS = 200;
	private static final int BURST_ADMITTED = 5;
	private static final int FREE_SESSIONS_PER_DAY = 3;

	@Test
	void burstLimitAdmitsExactlyFiveOfTwoHundredParallelAttempts() {
		AtomicLong attempts = new AtomicLong();
		RateLimitRepository counters = mock(RateLimitRepository.class);
		when(counters.incrementBurst(eq(STUDENT), any())).thenAnswer(call -> attempts.incrementAndGet());
		BurstLimiter burst = new BurstLimiter(counters);

		List<Optional<ErrorCode>> outcomes =
				inParallel(ATTEMPTS, () -> attempt(() -> burst.assertWithinBurst(STUDENT)));

		assertThat(outcomes).filteredOn(Optional::isEmpty).hasSize(BURST_ADMITTED);
		assertThat(outcomes).filteredOn(Optional::isPresent)
				.extracting(Optional::get)
				.containsOnly(ErrorCode.RATE_LIMITED);
	}

	@Test
	void freePlanNeverHoldsMoreThanItsDailySessionsAtOnce() {
		AtomicLong reserved = new AtomicLong();
		RateLimitRepository counters = mock(RateLimitRepository.class);
		when(counters.incrementDaily(eq(STUDENT), any())).thenAnswer(call -> reserved.incrementAndGet());
		doAnswer(call -> reserved.decrementAndGet()).when(counters).releaseDaily(eq(STUDENT), any());
		UserService users = mock(UserService.class);
		when(users.currentPlan(STUDENT)).thenReturn(Plan.FREE);
		BurstLimiter burst = mock(BurstLimiter.class);
		RateLimiterService limiter = new RateLimiterService(users, counters, burst, CLOCK);

		List<Optional<ErrorCode>> outcomes = inParallel(ATTEMPTS, () -> attempt(() -> limiter.reserveSession(STUDENT)));

		assertThat(outcomes).filteredOn(Optional::isEmpty).hasSize(FREE_SESSIONS_PER_DAY);
		assertThat(outcomes).filteredOn(Optional::isPresent)
				.extracting(Optional::get)
				.containsOnly(ErrorCode.DAILY_LIMIT_REACHED);
		assertThat(reserved).hasValue(FREE_SESSIONS_PER_DAY);
	}

	/** Starts every task at the same moment, then collects one outcome per task. */
	private static <T> List<T> inParallel(int tasks, Supplier<T> task) {
		ExecutorService pool = Executors.newFixedThreadPool(THREADS);
		CountDownLatch go = new CountDownLatch(1);
		try {
			List<CompletableFuture<T>> futures = IntStream.range(0, tasks)
					.mapToObj(index -> CompletableFuture.supplyAsync(() -> afterStart(go, task), pool))
					.toList();
			go.countDown();
			return futures.stream().map(CompletableFuture::join).toList();
		} finally {
			pool.shutdownNow();
		}
	}

	private static <T> T afterStart(CountDownLatch go, Supplier<T> task) {
		try {
			go.await();
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted before the load started", exception);
		}
		return task.get();
	}

	private static Optional<ErrorCode> attempt(Runnable action) {
		try {
			action.run();
			return Optional.empty();
		} catch (ApiException exception) {
			return Optional.of(exception.getCode());
		}
	}
}
