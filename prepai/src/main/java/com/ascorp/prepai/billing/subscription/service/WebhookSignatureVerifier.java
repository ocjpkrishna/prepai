package com.ascorp.prepai.billing.subscription.service;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/** Checks Razorpay's X-Razorpay-Signature: hex HMAC-SHA256 of the raw body under the webhook secret. */
@Component
@RequiredArgsConstructor
public class WebhookSignatureVerifier {

	private static final String ALGORITHM = "HmacSHA256";

	private final RazorpayProperties properties;

	/** False for a missing signature, a missing secret (nothing can be trusted then) or a wrong signature. */
	public boolean isValid(String body, String signature) {
		String secret = properties.webhookSecret();
		if (secret == null || secret.isBlank() || signature == null) {
			return false;
		}
		byte[] expected = sign(body, secret).getBytes(StandardCharsets.UTF_8);
		return MessageDigest.isEqual(expected, signature.getBytes(StandardCharsets.UTF_8));
	}

	private String sign(String body, String secret) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), ALGORITHM));
			return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
		} catch (GeneralSecurityException e) {
			throw new IllegalStateException("HMAC-SHA256 is not available", e);
		}
	}
}
