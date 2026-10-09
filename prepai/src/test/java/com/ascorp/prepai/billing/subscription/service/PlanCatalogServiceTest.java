package com.ascorp.prepai.billing.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.billing.subscription.model.dto.PlanDto;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.model.enums.Plan;
import org.junit.jupiter.api.Test;

class PlanCatalogServiceTest {

	private final PlanCatalogService catalog = new PlanCatalogService();

	@Test
	void listsThreePlansWithSpecPrices() {
		assertThat(catalog.plans()).extracting(PlanDto::id, PlanDto::priceInr)
				.containsExactly(org.assertj.core.groups.Tuple.tuple("free", 0),
						org.assertj.core.groups.Tuple.tuple("pro", 199),
						org.assertj.core.groups.Tuple.tuple("pro_plus", 399));
	}

	@Test
	void proPlusHasUnlimitedSessions() {
		assertThat(catalog.plans().get(2).sessionsPerDay()).isNull();
	}

	@Test
	void resolvesPaidPlansIgnoringCase() {
		assertThat(catalog.paidPlan("PRO")).isEqualTo(Plan.PRO);
		assertThat(catalog.paidPlan("pro_plus")).isEqualTo(Plan.PRO_PLUS);
	}

	@Test
	void rejectsFreeAndUnknownPlans() {
		assertThatThrownBy(() -> catalog.paidPlan("free")).isInstanceOf(ApiException.class);
		assertThatThrownBy(() -> catalog.paidPlan("gold")).isInstanceOf(ApiException.class);
	}
}
