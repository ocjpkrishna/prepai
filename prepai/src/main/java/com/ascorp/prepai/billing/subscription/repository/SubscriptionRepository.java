package com.ascorp.prepai.billing.subscription.repository;

import com.ascorp.prepai.billing.subscription.model.entity.Subscription;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, UUID> {

	Optional<Subscription> findFirstByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<Subscription> findByRazorpaySubscriptionId(String razorpaySubscriptionId);
}
