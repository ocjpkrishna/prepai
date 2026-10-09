package com.ascorp.prepai.billing.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.common.model.enums.Plan;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FakeRazorpayGatewayTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final UUID NEXT_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");

	@Test
	void inventsASubscriptionIdFromTheIdSupplier() {
		FakeRazorpayGateway gateway = new FakeRazorpayGateway(() -> NEXT_ID);

		assertThat(gateway.createSubscription(Plan.PRO, USER_ID)).isEqualTo("sub_fake_" + NEXT_ID);
	}
}
