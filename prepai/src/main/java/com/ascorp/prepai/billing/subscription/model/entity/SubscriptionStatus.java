package com.ascorp.prepai.billing.subscription.model.entity;

/** Where a subscription is in its life: CREATED at checkout, ACTIVE once paid, CANCELLED when it ends. */
public enum SubscriptionStatus {
	CREATED, ACTIVE, CANCELLED
}
