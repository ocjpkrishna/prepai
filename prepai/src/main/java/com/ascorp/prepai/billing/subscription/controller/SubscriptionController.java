package com.ascorp.prepai.billing.subscription.controller;

import com.ascorp.prepai.billing.subscription.model.dto.CheckoutRequest;
import com.ascorp.prepai.billing.subscription.model.dto.CheckoutResponse;
import com.ascorp.prepai.billing.subscription.model.dto.PlanDto;
import com.ascorp.prepai.billing.subscription.model.dto.SubscriptionDto;
import com.ascorp.prepai.billing.subscription.service.PlanCatalogService;
import com.ascorp.prepai.billing.subscription.service.SubscriptionService;
import com.ascorp.prepai.billing.subscription.service.SubscriptionStatusService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Plans, checkout and the student's subscription (spec 4.4). Only the plan list is public. */
@RestController
@RequestMapping("/api/v1/subscriptions")
@RequiredArgsConstructor
public class SubscriptionController {

	private final PlanCatalogService catalog;
	private final SubscriptionService subscriptions;
	private final SubscriptionStatusService status;

	@GetMapping("/plans")
	public List<PlanDto> plans() {
		return catalog.plans();
	}

	@PostMapping("/checkout")
	public CheckoutResponse checkout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest request) {
		return subscriptions.checkout(UUID.fromString(jwt.getSubject()), request);
	}

	@GetMapping("/me")
	public SubscriptionDto mine(@AuthenticationPrincipal Jwt jwt) {
		return status.mine(UUID.fromString(jwt.getSubject()));
	}
}
