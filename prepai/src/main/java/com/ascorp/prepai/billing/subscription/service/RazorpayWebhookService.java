package com.ascorp.prepai.billing.subscription.service;

import com.ascorp.prepai.billing.subscription.model.entity.ProcessedWebhookEvent;
import com.ascorp.prepai.billing.subscription.repository.ProcessedWebhookEventRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Clock;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Handles Razorpay's callback: checks the signature, handles each event id once, then applies the event.
 * The event is recorded in the same transaction as its effect, so a failed delivery is retried whole.
 */
@Service
@RequiredArgsConstructor
public class RazorpayWebhookService {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private final WebhookSignatureVerifier verifier;
	private final ProcessedWebhookEventRepository processed;
	private final SubscriptionLifecycleService lifecycle;
	private final Clock clock;

	@Transactional
	public void handle(String rawBody, String signature, String eventId) {
		requireValidSignature(rawBody, signature);
		if (eventId == null || eventId.isBlank()) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED, "The event id is missing.");
		}
		if (processed.existsById(eventId)) {
			return;
		}
		JsonNode event = parse(rawBody);
		lifecycle.apply(event.path("event").asString(""), event);
		processed.save(new ProcessedWebhookEvent(eventId, clock.instant()));
	}

	private void requireValidSignature(String rawBody, String signature) {
		if (!verifier.isValid(rawBody, signature)) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED, "The signature is not valid.");
		}
	}

	private JsonNode parse(String rawBody) {
		try {
			return JSON.readTree(rawBody);
		} catch (JacksonException e) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED, "The event is not valid JSON.");
		}
	}
}
