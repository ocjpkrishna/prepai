package com.ascorp.prepai.billing.subscription.service;

import com.ascorp.prepai.common.model.enums.Plan;
import java.util.UUID;

/** Everything billing asks of Razorpay. Behind an interface so tests never reach the real service. */
public interface RazorpayGateway {

	/** Creates a recurring subscription for the student and returns Razorpay's id for it. */
	String createSubscription(Plan plan, UUID userId);
}
