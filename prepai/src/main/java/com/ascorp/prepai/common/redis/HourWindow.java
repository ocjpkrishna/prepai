package com.ascorp.prepai.common.redis;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/** Fixed one-hour windows, as used by the hourly Redis keys (spec 5.3). */
public final class HourWindow {

	public static final Duration LENGTH = Duration.ofHours(1);

	private HourWindow() {
	}

	/** The first instant of the hour that contains {@code now}. */
	public static Instant start(Instant now) {
		return now.truncatedTo(ChronoUnit.HOURS);
	}

	/** Whole seconds until the current hour ends, so a client knows when to try again. */
	public static long secondsLeft(Instant now) {
		return Duration.between(now, start(now).plus(LENGTH)).toSeconds();
	}
}
