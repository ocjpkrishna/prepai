package com.ascorp.prepai.billing.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.user.service.UserService;
import com.ascorp.prepai.billing.subscription.model.dto.SubscriptionDto;
import com.ascorp.prepai.billing.subscription.model.entity.Subscription;
import com.ascorp.prepai.billing.subscription.repository.SubscriptionRepository;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SubscriptionStatusServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final Instant NOW = Instant.parse("2026-10-09T10:00:00Z");

	@Mock
	private SubscriptionRepository subscriptions;

	@Mock
	private UserService users;

	private SubscriptionStatusService service;

	@BeforeEach
	void setUp() {
		service = new SubscriptionStatusService(subscriptions, users);
	}

	@Test
	void mineReportsNoneForAStudentWhoNeverBought() {
		when(users.currentPlan(USER_ID)).thenReturn(Plan.FREE);
		when(subscriptions.findFirstByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Optional.empty());

		assertThat(service.mine(USER_ID)).isEqualTo(new SubscriptionDto(Plan.FREE, "NONE", null));
	}

	@Test
	void mineKeepsThePlanTheStudentReallyHasUntilPaymentArrives() {
		Subscription unpaid = new Subscription(UUID.randomUUID(), USER_ID, Plan.PRO, "sub_1", NOW);
		when(users.currentPlan(USER_ID)).thenReturn(Plan.FREE);
		when(subscriptions.findFirstByUserIdOrderByCreatedAtDesc(USER_ID)).thenReturn(Optional.of(unpaid));

		assertThat(service.mine(USER_ID)).isEqualTo(new SubscriptionDto(Plan.FREE, "CREATED", null));
	}
}
