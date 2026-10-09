package com.ascorp.prepai.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.account.auth.config.AuthProperties;
import com.ascorp.prepai.account.auth.config.JwtConfig;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AccessTokenEncoderTest {

	private static final UUID USER = UUID.fromString("6a9d7c8d-1d2e-4a3b-8c4d-6e7f8091a2b3");
	private static final int ACCESS_HOURS = 1;

	@Test
	void encodedTokenNamesTheUserAndExpiresWithinTheAccessLifetime() {
		AuthProperties properties = new AuthProperties(
				new AuthProperties.Jwt("test-only-signing-secret-for-unit-tests-0123456789", ACCESS_HOURS, 30),
				new AuthProperties.Google("client"));
		JwtConfig config = new JwtConfig(properties);
		AccessTokenEncoder encoder = new AccessTokenEncoder(config.jwtEncoder(), properties, Clock.systemUTC());
		Instant before = Instant.now();

		Jwt jwt = config.jwtDecoder().decode(encoder.encode(USER));

		assertThat(jwt.getSubject()).isEqualTo(USER.toString());
		assertThat(jwt.getExpiresAt()).isAfter(before)
				.isBeforeOrEqualTo(before.plus(Duration.ofHours(ACCESS_HOURS)).plusSeconds(1));
	}
}
