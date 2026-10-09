package com.ascorp.prepai.common.model.enums;

import lombok.Getter;

/** The subscription tiers and their daily limits (spec 1.4). A null session cap means unlimited. */
@Getter
public enum Plan {

	FREE(Limits.FREE_SESSIONS_PER_DAY, Limits.FREE_SESSION_MINUTES),
	PRO(Limits.PRO_SESSIONS_PER_DAY, Limits.PRO_SESSION_MINUTES),
	PRO_PLUS(null, Limits.PRO_PLUS_SESSION_MINUTES);

	private final Integer sessionsPerDay;
	private final int maxSessionMinutes;

	Plan(Integer sessionsPerDay, int maxSessionMinutes) {
		this.sessionsPerDay = sessionsPerDay;
		this.maxSessionMinutes = maxSessionMinutes;
	}

	private static final class Limits {

		static final int FREE_SESSIONS_PER_DAY = 3;
		static final int FREE_SESSION_MINUTES = 5;
		static final int PRO_SESSIONS_PER_DAY = 30;
		static final int PRO_SESSION_MINUTES = 20;
		static final int PRO_PLUS_SESSION_MINUTES = 30;

		private Limits() {
		}
	}
}
