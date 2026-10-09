package com.ascorp.prepai.billing.subscription.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Razorpay settings (prepai.razorpay.*). Values come from the environment only; they are empty without keys. */
@ConfigurationProperties("prepai.razorpay")
public record RazorpayProperties(String keyId, String keySecret, String webhookSecret) {
}
