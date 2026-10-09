package com.ascorp.prepai.billing.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.user.service.UserService;
import com.ascorp.prepai.billing.subscription.model.entity.Subscription;
import com.ascorp.prepai.billing.subscription.model.entity.SubscriptionStatus;
import com.ascorp.prepai.billing.subscription.repository.ProcessedWebhookEventRepository;
import com.ascorp.prepai.billing.subscription.repository.SubscriptionRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RazorpayWebhookServiceTest {

	private static final String SECRET = "whsec";
	private static final String EVENT_ID = "evt_1";
	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");
	private static final long PERIOD_START = 1_760_000_000L;
	private static final long PERIOD_END = 1_762_592_000L;

	@Mock
	private ProcessedWebhookEventRepository processed;

	@Mock
	private SubscriptionRepository subscriptions;

	@Mock
	private UserService users;

	private RazorpayWebhookService service;
	private Subscription subscription;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		SubscriptionLifecycleService lifecycle = new SubscriptionLifecycleService(subscriptions, users, clock);
		WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(new RazorpayProperties("", "", SECRET));
		service = new RazorpayWebhookService(verifier, processed, lifecycle, clock);
		subscription = new Subscription(UUID.randomUUID(), USER_ID, Plan.PRO, "sub_1", NOW);
	}

	@Test
	void paymentActivatesTheSubscriptionAndUpgradesTheStudent() throws Exception {
		when(processed.existsById(EVENT_ID)).thenReturn(false);
		when(subscriptions.findByRazorpaySubscriptionId("sub_1")).thenReturn(Optional.of(subscription));
		String body = event("subscription.charged");

		service.handle(body, WebhookSignatureVerifierTest.sign(body, SECRET), EVENT_ID);

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
		verify(users).changePlan(USER_ID, Plan.PRO, Instant.ofEpochSecond(PERIOD_END));
		verify(processed).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void cancellationDropsTheStudentToFree() throws Exception {
		when(processed.existsById(EVENT_ID)).thenReturn(false);
		when(subscriptions.findByRazorpaySubscriptionId("sub_1")).thenReturn(Optional.of(subscription));
		String body = event("subscription.cancelled");

		service.handle(body, WebhookSignatureVerifierTest.sign(body, SECRET), EVENT_ID);

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
		verify(users).changePlan(USER_ID, Plan.FREE, null);
	}

	@Test
	void aLatePaymentEventDoesNotReviveACancelledSubscription() throws Exception {
		subscription.cancel(NOW);
		when(processed.existsById(EVENT_ID)).thenReturn(false);
		when(subscriptions.findByRazorpaySubscriptionId("sub_1")).thenReturn(Optional.of(subscription));
		String body = event("subscription.charged");

		service.handle(body, WebhookSignatureVerifierTest.sign(body, SECRET), EVENT_ID);

		assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
		verifyNoInteractions(users);
		verify(processed).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void aRepeatedEventIdChangesNothing() throws Exception {
		when(processed.existsById(EVENT_ID)).thenReturn(true);
		String body = event("subscription.charged");

		service.handle(body, WebhookSignatureVerifierTest.sign(body, SECRET), EVENT_ID);

		verifyNoInteractions(users, subscriptions);
		verify(processed, never()).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void aWrongSignatureIsRejectedBeforeAnythingHappens() {
		assertThatThrownBy(() -> service.handle(event("subscription.charged"), "bad", EVENT_ID))
				.isInstanceOf(ApiException.class);
		verifyNoInteractions(users, subscriptions, processed);
	}

	@Test
	void aMissingEventIdIsRejected() throws Exception {
		String body = event("subscription.charged");
		String signature = WebhookSignatureVerifierTest.sign(body, SECRET);

		assertThatThrownBy(() -> service.handle(body, signature, null)).isInstanceOf(ApiException.class);
	}

	@Test
	void anEventForAnUnknownSubscriptionIsRecordedButChangesNoPlan() throws Exception {
		when(processed.existsById(EVENT_ID)).thenReturn(false);
		when(subscriptions.findByRazorpaySubscriptionId("sub_1")).thenReturn(Optional.empty());
		String body = event("subscription.charged");

		service.handle(body, WebhookSignatureVerifierTest.sign(body, SECRET), EVENT_ID);

		verifyNoInteractions(users);
		verify(processed).save(org.mockito.ArgumentMatchers.any());
	}

	@Test
	void malformedJsonIsRejected() throws Exception {
		when(processed.existsById(EVENT_ID)).thenReturn(false);
		String body = "{not json";
		String signature = WebhookSignatureVerifierTest.sign(body, SECRET);

		assertThatThrownBy(() -> service.handle(body, signature, EVENT_ID)).isInstanceOf(ApiException.class);
	}

	private static String event(String type) {
		return "{\"event\":\"" + type + "\",\"payload\":{\"subscription\":{\"entity\":{\"id\":\"sub_1\","
				+ "\"current_start\":" + PERIOD_START + ",\"current_end\":" + PERIOD_END + "}}}}";
	}
}
