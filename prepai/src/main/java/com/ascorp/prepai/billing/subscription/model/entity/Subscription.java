package com.ascorp.prepai.billing.subscription.model.entity;

import com.ascorp.prepai.common.model.enums.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** One purchase of a paid plan through Razorpay (spec 5.1). */
@Entity
@Table(name = "subscriptions")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Subscription {

	@Id
	private UUID id;

	@Column(nullable = false)
	private UUID userId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Plan plan;

	private String razorpaySubscriptionId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private SubscriptionStatus status;

	private Instant currentPeriodStart;

	private Instant currentPeriodEnd;

	@Column(nullable = false)
	private Instant createdAt;

	private Instant cancelledAt;

	public Subscription(UUID id, UUID userId, Plan plan, String razorpaySubscriptionId, Instant createdAt) {
		this.id = id;
		this.userId = userId;
		this.plan = plan;
		this.razorpaySubscriptionId = razorpaySubscriptionId;
		this.status = SubscriptionStatus.CREATED;
		this.createdAt = createdAt;
	}

	public void activate(Instant periodStart, Instant periodEnd) {
		this.status = SubscriptionStatus.ACTIVE;
		this.currentPeriodStart = periodStart;
		this.currentPeriodEnd = periodEnd;
		this.cancelledAt = null;
	}

	public void cancel(Instant now) {
		this.status = SubscriptionStatus.CANCELLED;
		this.cancelledAt = now;
	}
}
