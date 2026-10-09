package com.ascorp.prepai.billing.subscription.model.dto;

import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Instant;

/** The student's current plan; {@code status} is NONE for a student who never bought one. */
public record SubscriptionDto(Plan plan, String status, Instant currentPeriodEnd) {
}
