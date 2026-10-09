package com.ascorp.prepai.billing.subscription.service;

import com.ascorp.prepai.common.model.enums.Plan;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Stands in for Razorpay until test keys exist (decision 6): it only invents a subscription id. */
@Component
@RequiredArgsConstructor
public class FakeRazorpayGateway implements RazorpayGateway {

	private static final String ID_PREFIX = "sub_fake_";

	private final Supplier<UUID> ids;

	@Override
	public String createSubscription(Plan plan, UUID userId) {
		return ID_PREFIX + ids.get();
	}
}
