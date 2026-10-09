package com.ascorp.prepai.billing.subscription.controller;

import com.ascorp.prepai.billing.subscription.service.RazorpayWebhookService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Razorpay's callback (spec 4.4). Public, because Razorpay has no token; the signature is the proof. */
@RestController
@RequestMapping("/api/v1/subscriptions/webhook")
@RequiredArgsConstructor
public class RazorpayWebhookController {

	private final RazorpayWebhookService webhooks;

	@PostMapping
	@ResponseStatus(HttpStatus.OK)
	public void receive(@RequestBody String rawBody,
			@RequestHeader(value = "X-Razorpay-Signature", required = false) String signature,
			@RequestHeader(value = "X-Razorpay-Event-Id", required = false) String eventId) {
		webhooks.handle(rawBody, signature, eventId);
	}
}
