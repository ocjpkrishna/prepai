package com.ascorp.prepai.billing.subscription.service;

import com.ascorp.prepai.account.user.service.UserService;
import com.ascorp.prepai.billing.subscription.model.dto.SubscriptionDto;
import com.ascorp.prepai.billing.subscription.repository.SubscriptionRepository;
import com.ascorp.prepai.common.model.enums.Plan;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Reports the student's current plan and the state of their latest purchase (spec 4.4). */
@Service
@RequiredArgsConstructor
public class SubscriptionStatusService {

	private static final String NO_SUBSCRIPTION = "NONE";

	private final SubscriptionRepository subscriptions;
	private final UserService users;

	/** The plan is the one the student really has now; a checkout that was never paid does not change it. */
	@Transactional(readOnly = true)
	public SubscriptionDto mine(UUID userId) {
		Plan plan = users.currentPlan(userId);
		return subscriptions.findFirstByUserIdOrderByCreatedAtDesc(userId)
				.map(latest -> new SubscriptionDto(plan, latest.getStatus().name(), latest.getCurrentPeriodEnd()))
				.orElseGet(() -> new SubscriptionDto(plan, NO_SUBSCRIPTION, null));
	}
}
