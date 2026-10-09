package com.ascorp.prepai.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.account.auth.service.GoogleTokenVerifier.GoogleIdentity;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

@ExtendWith(MockitoExtension.class)
class GoogleTokenVerifierTest {

	private static final String CLIENT_ID = "our-client-id";
	private static final String ISSUER = "https://accounts.google.com";
	private static final String ID_TOKEN = "google-id-token";

	@Mock
	private JwtDecoder decoder;

	@Test
	void verifyReturnsTheIdentityOfAVerifiedGoogleAccount() {
		when(decoder.decode(ID_TOKEN)).thenReturn(googleJwt(true, List.of(CLIENT_ID), ISSUER));

		GoogleIdentity identity = new GoogleTokenVerifier(decoder).verify(ID_TOKEN);

		assertThat(identity).isEqualTo(new GoogleIdentity("google-subject", "asha@example.com", "Asha"));
	}

	@Test
	void verifyRejectsAnAccountWhoseEmailIsNotVerified() {
		when(decoder.decode(ID_TOKEN)).thenReturn(googleJwt(false, List.of(CLIENT_ID), ISSUER));

		assertUnauthenticated(new GoogleTokenVerifier(decoder));
	}

	@Test
	void verifyRejectsATokenThatFailsDecoding() {
		when(decoder.decode(ID_TOKEN)).thenThrow(new JwtException("expired"));

		assertUnauthenticated(new GoogleTokenVerifier(decoder));
	}

	@Test
	void validatorAcceptsOnlyTokensIssuedForOurClient() {
		Jwt forUs = googleJwt(true, List.of(CLIENT_ID), ISSUER);
		Jwt forSomeoneElse = googleJwt(true, List.of("another-app"), ISSUER);

		assertThat(GoogleTokenVerifier.googleValidator(CLIENT_ID).validate(forUs).hasErrors()).isFalse();
		assertThat(GoogleTokenVerifier.googleValidator(CLIENT_ID).validate(forSomeoneElse).hasErrors()).isTrue();
	}

	@Test
	void validatorRejectsAnotherIssuer() {
		Jwt fromElsewhere = googleJwt(true, List.of(CLIENT_ID), "https://evil.example.com");

		OAuth2TokenValidatorResult result = GoogleTokenVerifier.googleValidator(CLIENT_ID).validate(fromElsewhere);

		assertThat(result.hasErrors()).isTrue();
	}

	private static void assertUnauthenticated(GoogleTokenVerifier verifier) {
		assertThatThrownBy(() -> verifier.verify(ID_TOKEN))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
	}

	private static Jwt googleJwt(boolean emailVerified, List<String> audience, String issuer) {
		Instant now = Instant.now();
		return Jwt.withTokenValue(ID_TOKEN)
				.header("alg", "RS256")
				.subject("google-subject")
				.claim("email", "asha@example.com")
				.claim("email_verified", emailVerified)
				.claim("name", "Asha")
				.claim(JwtClaimNames.AUD, audience)
				.claim(JwtClaimNames.ISS, issuer)
				.issuedAt(now)
				.expiresAt(now.plus(Duration.ofHours(1)))
				.build();
	}
}
