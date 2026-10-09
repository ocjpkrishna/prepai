package com.ascorp.prepai.account.auth.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/** Signs and checks PrepAI's own access tokens with one HS256 key (spec 4.1). */
@Configuration
@RequiredArgsConstructor
public class JwtConfig {

	private static final String HMAC_ALGORITHM = "HmacSHA256";

	private final AuthProperties properties;

	@Bean
	public JwtEncoder jwtEncoder() {
		return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey()));
	}

	@Bean
	public JwtDecoder jwtDecoder() {
		return NimbusJwtDecoder.withSecretKey(secretKey()).macAlgorithm(MacAlgorithm.HS256).build();
	}

	private SecretKey secretKey() {
		byte[] secret = properties.jwt().secret().getBytes(StandardCharsets.UTF_8);
		return new SecretKeySpec(secret, HMAC_ALGORITHM);
	}
}
