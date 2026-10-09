package com.ascorp.prepai.billing.subscription.service;

import com.ascorp.prepai.account.user.service.UserService;
import com.ascorp.prepai.billing.subscription.model.entity.Subscription;
import com.ascorp.prepai.billing.subscription.repository.SubscriptionRepository;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;

/** Applies one verified Razorpay subscription event: a payment starts or renews the plan, an end drops it to FREE. */
@Service
@RequiredArgsConstructor
public class SubscriptionLifecycleService {

	private static final Set<String> PAID_EVENTS = Set.of("subscription.activated", "subscription.charged");
	private static final Set<String> ENDED_EVENTS = Set.of("subscription.cancelled", "subscription.halted",
			"subscription.completed", "subscription.expired");
	private static final Duration DEFAULT_PERIOD = Duration.ofDays(30);

	private final SubscriptionRepository subscriptions;
	private final UserService users;
	private final Clock clock;

	/** Events for other kinds of payment, or for a subscription we never created, are ignored. */
	public void apply(String eventType, JsonNode event) {
		JsonNode entity = event.path("payload").path("subscription").path("entity");
		subscriptions.findByRazorpaySubscriptionId(entity.path("id").asString(""))
				.ifPresent(subscription -> applyTo(subscription, eventType, entity));
	}

	private void applyTo(Subscription subscription, String eventType, JsonNode entity) {
		if (PAID_EVENTS.contains(eventType)) {
			start(subscription, entity);
		} else if (ENDED_EVENTS.contains(eventType)) {
			end(subscription);
		}
	}

	private void start(Subscription subscription, JsonNode entity) {
		Instant now = clock.instant();
		Instant periodStart = instantOf(entity.path("current_start"), now);
		Instant periodEnd = instantOf(entity.path("current_end"), periodStart.plus(DEFAULT_PERIOD));
		subscription.activate(periodStart, periodEnd);
		users.changePlan(subscription.getUserId(), subscription.getPlan(), periodEnd);
	}

	private void end(Subscription subscription) {
		subscription.cancel(clock.instant());
		users.changePlan(subscription.getUserId(), Plan.FREE, null);
	}

	private Instant instantOf(JsonNode epochSeconds, Instant fallback) {
		return epochSeconds.isNumber() ? Instant.ofEpochSecond(epochSeconds.asLong()) : fallback;
	}
}
