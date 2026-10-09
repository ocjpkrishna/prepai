package com.ascorp.prepai.billing.subscription.service;

import com.ascorp.prepai.billing.subscription.mapper.SubscriptionMapper;
import com.ascorp.prepai.billing.subscription.model.dto.CheckoutRequest;
import com.ascorp.prepai.billing.subscription.model.dto.CheckoutResponse;
import com.ascorp.prepai.billing.subscription.model.entity.Subscription;
import com.ascorp.prepai.billing.subscription.repository.SubscriptionRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Starts a purchase (spec 4.4). The webhook finishes it. */
@Service
@RequiredArgsConstructor
public class SubscriptionService {

	private static final String PAYMENT_METHOD = "razorpay";

	private final PlanCatalogService catalog;
	private final RazorpayGateway gateway;
	private final SubscriptionRepository subscriptions;
	private final SubscriptionMapper mapper;
	private final Supplier<UUID> ids;
	private final Clock clock;

	@Transactional
	public CheckoutResponse checkout(UUID userId, CheckoutRequest request) {
		requireRazorpay(request.paymentMethod());
		Plan plan = catalog.paidPlan(request.planId());
		String razorpayId = gateway.createSubscription(plan, userId);
		Subscription created = new Subscription(ids.get(), userId, plan, razorpayId, clock.instant());
		return mapper.toCheckout(subscriptions.save(created));
	}

	private void requireRazorpay(String paymentMethod) {
		if (!PAYMENT_METHOD.equalsIgnoreCase(paymentMethod)) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED, "Only Razorpay payments are supported.");
		}
	}
}
