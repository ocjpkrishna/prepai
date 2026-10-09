package com.ascorp.prepai.billing.subscription.model.dto;

import java.util.UUID;

/** What the frontend needs to open Razorpay's checkout for the new subscription. */
public record CheckoutResponse(UUID subscriptionId, String razorpaySubscriptionId, String planId) {
}
