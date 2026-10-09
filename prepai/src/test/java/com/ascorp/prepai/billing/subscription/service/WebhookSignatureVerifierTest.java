package com.ascorp.prepai.billing.subscription.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebhookSignatureVerifierTest {

	private static final String SECRET = "test-secret";
	private static final String BODY = "{\"event\":\"subscription.charged\"}";

	private final WebhookSignatureVerifier verifier = new WebhookSignatureVerifier(
			new RazorpayProperties("id", "key", SECRET));

	@Test
	void acceptsTheCorrectSignature() throws Exception {
		assertThat(verifier.isValid(BODY, sign(BODY, SECRET))).isTrue();
	}

	@Test
	void rejectsSignatureOfAnotherBodyOrSecret() throws Exception {
		assertThat(verifier.isValid(BODY, sign("{}", SECRET))).isFalse();
		assertThat(verifier.isValid(BODY, sign(BODY, "other"))).isFalse();
	}

	@Test
	void rejectsMissingSignature() {
		assertThat(verifier.isValid(BODY, null)).isFalse();
	}

	@Test
	void rejectsEverythingWithoutASecret() throws Exception {
		WebhookSignatureVerifier unconfigured = new WebhookSignatureVerifier(new RazorpayProperties("", "", ""));
		assertThat(unconfigured.isValid(BODY, sign(BODY, SECRET))).isFalse();
	}

	static String sign(String body, String secret) throws Exception {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
	}
}
