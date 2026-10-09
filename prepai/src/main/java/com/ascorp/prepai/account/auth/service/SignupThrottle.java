package com.ascorp.prepai.account.auth.service;

import com.ascorp.prepai.account.auth.config.SignupProperties;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.redis.HourWindow;
import com.ascorp.prepai.common.redis.RedisCounter;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Limits sign-ups per IP address per hour, counted in Redis under {@code signup:{ip}:{hour}} (spec 5.3). */
@Service
@RequiredArgsConstructor
public class SignupThrottle {

	private static final String RETRY_MESSAGE = "Too many sign-ups from this network. Try again later.";

	private final RedisCounter counters;
	private final SignupProperties settings;
	private final Clock clock;

	/** Counts one sign-up from this address and refuses it once the hour's allowance is used. */
	public void assertWithinSignupLimit(String ip) {
		Instant now = clock.instant();
		String key = "signup:" + ip + ":" + HourWindow.start(now).getEpochSecond();
		if (counters.increment(key, HourWindow.LENGTH) > settings.signupsPerIpPerHour()) {
			throw new ApiException(ErrorCode.RATE_LIMITED, RETRY_MESSAGE, HourWindow.secondsLeft(now));
		}
	}
}
