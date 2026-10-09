package com.ascorp.prepai.billing.subscription.model.dto;

/** A plan as the pricing page shows it (spec 1.4). {@code sessionsPerDay} is null when unlimited. */
public record PlanDto(String id, String name, int priceInr, Integer sessionsPerDay, int maxSessionMinutes) {
}
