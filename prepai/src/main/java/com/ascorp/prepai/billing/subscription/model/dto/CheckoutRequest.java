package com.ascorp.prepai.billing.subscription.model.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(@NotBlank String planId, @NotBlank String paymentMethod) {
}
