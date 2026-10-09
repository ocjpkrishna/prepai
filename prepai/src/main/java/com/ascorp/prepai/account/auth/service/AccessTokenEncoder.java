package com.ascorp.prepai.account.auth.service;

import com.ascorp.prepai.account.auth.config.AuthProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/** Signs the short-lived access token of a student. Its subject is the user id and nothing else (spec 4.1). */
@Component
@RequiredArgsConstructor
public class AccessTokenEncoder {

	private final JwtEncoder jwtEncoder;
	private final AuthProperties properties;
	private final Clock clock;

	public String encode(UUID userId) {
		Instant now = clock.instant();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(userId.toString())
				.issuedAt(now)
				.expiresAt(now.plus(properties.jwt().accessLifetime()))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
