package com.ascorp.prepai.billing.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.billing.subscription.mapper.SubscriptionMapperImpl;
import com.ascorp.prepai.billing.subscription.model.dto.CheckoutRequest;
import com.ascorp.prepai.billing.subscription.model.dto.CheckoutResponse;
import com.ascorp.prepai.billing.subscription.model.entity.Subscription;
import com.ascorp.prepai.billing.subscription.repository.SubscriptionRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final UUID SUBSCRIPTION_ID = UUID.fromString("11111111-2222-3333-4444-555555555555");
	private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");

	@Mock
	private RazorpayGateway gateway;

	@Mock
	private SubscriptionRepository subscriptions;

	private SubscriptionService service;

	@BeforeEach
	void setUp() {
		service = new SubscriptionService(new PlanCatalogService(), gateway, subscriptions,
				new SubscriptionMapperImpl(), () -> SUBSCRIPTION_ID, Clock.fixed(NOW, ZoneOffset.UTC));
	}

	@Test
	void checkoutStoresACreatedSubscriptionAndReturnsRazorpaysId() {
		when(gateway.createSubscription(Plan.PRO, USER_ID)).thenReturn("sub_1");
		when(subscriptions.save(any(Subscription.class))).thenAnswer(call -> call.getArgument(0));

		CheckoutResponse response = service.checkout(USER_ID, new CheckoutRequest("pro", "razorpay"));

		assertThat(response).isEqualTo(new CheckoutResponse(SUBSCRIPTION_ID, "sub_1", "pro"));
	}

	@Test
	void checkoutRejectsOtherPaymentMethods() {
		assertThatThrownBy(() -> service.checkout(USER_ID, new CheckoutRequest("pro", "card")))
				.isInstanceOf(ApiException.class);
	}

	@Test
	void checkoutRejectsTheFreePlan() {
		assertThatThrownBy(() -> service.checkout(USER_ID, new CheckoutRequest("free", "razorpay")))
				.isInstanceOf(ApiException.class);
	}
}
