package com.ascorp.prepai.adversarial;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ascorp.prepai.account.auth.model.entity.RefreshToken;
import com.ascorp.prepai.billing.subscription.model.entity.ProcessedWebhookEvent;
import com.ascorp.prepai.support.ApiTestSupport;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Hostile requests over HTTP: malformed and oversized bodies, bad, expired and replayed tokens, and webhooks with
 * forged or repeated signatures. Each one must fail closed with the spec 4.7 error shape and change nothing.
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = "prepai.razorpay.webhook-secret=" + AdversarialRequestTest.WEBHOOK_SECRET)
class AdversarialRequestTest extends ApiTestSupport {

	static final String WEBHOOK_SECRET = "test-only-webhook-secret";

	private static final String LOGIN = "/api/v1/auth/login";
	private static final String REGISTER = "/api/v1/auth/register";
	private static final String REFRESH = "/api/v1/auth/refresh";
	private static final String WEBHOOK = "/api/v1/subscriptions/webhook";
	private static final String ME = "/api/v1/users/me";
	private static final String JSON = "application/json";
	private static final String SIGNATURE = "X-Razorpay-Signature";
	private static final String EVENT_ID = "X-Razorpay-Event-Id";
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";
	private static final String CHARGED = """
			{"event":"subscription.charged","payload":{"subscription":{"entity":{"id":"sub_1"}}}}""";
	private static final int OVERSIZED_CHARS = 2_000_000;
	private static final Duration ONE_MINUTE = Duration.ofMinutes(1);
	private static final Duration ONE_HOUR = Duration.ofHours(1);

	@Test
	void malformedJsonIsAValidationFailureWithNoInternalDetail() throws Exception {
		send(post(LOGIN).contentType(JSON).content("{\"email\": "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"))
				.andExpect(content().string(not(containsString("Exception"))))
				.andExpect(content().string(not(containsString("jackson"))));
	}

	@Test
	void oversizedRegistrationNameIsRejectedBeforeAnythingIsSaved() throws Exception {
		String name = "a".repeat(OVERSIZED_CHARS);
		String body = "{\"email\":\"big@example.com\",\"password\":\"long-enough-1\",\"name\":\"" + name
				+ "\",\"birthDate\":\"2000-01-01\",\"acceptsTerms\":true}";

		send(post(REGISTER).contentType(JSON).content(body))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
		verify(users, never()).saveAndFlush(any());
	}

	@Test
	void expiredAccessTokenIsRejected() throws Exception {
		Instant now = Instant.now();
		String token = signedToken(UUID.randomUUID(), now.minus(ONE_HOUR), now.minus(ONE_MINUTE));

		send(get(ME).header(AUTHORIZATION, BEARER + token))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
	}

	@Test
	void tokenWithAForgedPayloadIsRejected() throws Exception {
		String genuine = signedToken();
		String[] parts = genuine.split("\\.");
		String forgedPayload = encode("{\"sub\":\"" + UUID.randomUUID() + "\",\"exp\":9999999999}");
		String forged = parts[0] + "." + forgedPayload + "." + parts[2];

		send(get(ME).header(AUTHORIZATION, BEARER + forged))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void unsignedAlgNoneTokenIsRejected() throws Exception {
		String genuine = signedToken();
		String[] parts = genuine.split("\\.");
		String unsigned = encode("{\"alg\":\"none\"}") + "." + parts[1] + ".";

		send(get(ME).header(AUTHORIZATION, BEARER + unsigned))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void replayedRefreshTokenIsRejectedAndItsWholeFamilyIsRevoked() throws Exception {
		UUID family = UUID.randomUUID();
		RefreshToken rotated = RefreshToken.builder()
				.id(UUID.randomUUID())
				.userId(UUID.randomUUID())
				.familyId(family)
				.tokenHash("hash")
				.expiresAt(Instant.now().plus(ONE_HOUR))
				.revokedAt(Instant.now().minus(ONE_MINUTE))
				.build();
		when(refreshTokens.findByTokenHash(any())).thenReturn(Optional.of(rotated));

		send(post(REFRESH).contentType(JSON).content("{\"refreshToken\":\"already-used\"}"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
		verify(refreshTokens).revokeFamily(org.mockito.ArgumentMatchers.eq(family), any());
	}

	@Test
	void webhookWithAForgedSignatureChangesNothing() throws Exception {
		send(post(WEBHOOK).contentType(JSON).content(CHARGED)
				.header(SIGNATURE, "00".repeat(32))
				.header(EVENT_ID, "evt_forged"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
		verify(processedWebhookEvents, never()).save(any());
		verify(subscriptions, never()).findByRazorpaySubscriptionId(any());
	}

	@Test
	void webhookWithoutASignatureIsRejected() throws Exception {
		send(post(WEBHOOK).contentType(JSON).content(CHARGED).header(EVENT_ID, "evt_unsigned"))
				.andExpect(status().isBadRequest());
		verify(processedWebhookEvents, never()).save(any());
	}

	@Test
	void validlySignedButMalformedWebhookBodyIsRejected() throws Exception {
		String body = "{not json";

		send(post(WEBHOOK).contentType(JSON).content(body)
				.header(SIGNATURE, hmacHex(body))
				.header(EVENT_ID, "evt_broken"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
		verify(processedWebhookEvents, never()).save(any());
	}

	@Test
	void aDuplicateWebhookEventIsAppliedOnlyOnce() throws Exception {
		when(processedWebhookEvents.existsById("evt_twice")).thenReturn(false, true);
		when(subscriptions.findByRazorpaySubscriptionId(any())).thenReturn(Optional.empty());

		for (int delivery = 0; delivery < 2; delivery++) {
			send(post(WEBHOOK).contentType(JSON).content(CHARGED)
					.header(SIGNATURE, hmacHex(CHARGED))
					.header(EVENT_ID, "evt_twice"))
					.andExpect(status().isOk());
		}
		verify(processedWebhookEvents, org.mockito.Mockito.times(1)).save(any(ProcessedWebhookEvent.class));
	}

	private ResultActions send(org.springframework.test.web.servlet.RequestBuilder request) throws Exception {
		return mvc.perform(request);
	}

	private static String encode(String json) {
		return Base64.getUrlEncoder().withoutPadding().encodeToString(json.getBytes(StandardCharsets.UTF_8));
	}

	private static String hmacHex(String body) throws GeneralSecurityException {
		Mac mac = Mac.getInstance("HmacSHA256");
		mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
		return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
	}
}
