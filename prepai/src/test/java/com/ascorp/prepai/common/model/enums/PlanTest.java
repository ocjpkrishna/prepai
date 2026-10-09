package com.ascorp.prepai.common.model.enums;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PlanTest {

	@Test
	void freePlanAllowsThreeSessionsOfFiveMinutes() {
		assertThat(Plan.FREE.getSessionsPerDay()).isEqualTo(3);
		assertThat(Plan.FREE.getMaxSessionMinutes()).isEqualTo(5);
	}

	@Test
	void proPlusHasNoDailySessionCap() {
		assertThat(Plan.PRO_PLUS.getSessionsPerDay()).isNull();
	}
}
