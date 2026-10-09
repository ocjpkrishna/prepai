package com.ascorp.prepai.billing.subscription.repository;

import com.ascorp.prepai.billing.subscription.model.entity.ProcessedWebhookEvent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessedWebhookEventRepository extends JpaRepository<ProcessedWebhookEvent, String> {
}
