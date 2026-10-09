package com.ascorp.prepai.billing.subscription.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A Razorpay event id we already handled; Razorpay retries deliveries, so each id must act only once. */
@Entity
@Table(name = "processed_webhook_events")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProcessedWebhookEvent {

	@Id
	private String eventId;

	@Column(nullable = false)
	private Instant processedAt;

	public ProcessedWebhookEvent(String eventId, Instant processedAt) {
		this.eventId = eventId;
		this.processedAt = processedAt;
	}
}
