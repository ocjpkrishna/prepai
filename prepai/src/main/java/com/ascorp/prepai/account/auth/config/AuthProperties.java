package com.ascorp.prepai.account.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Login settings (prepai.security.*). The secret and the client id come from the environment only. */
@ConfigurationProperties("prepai.security")
public record AuthProperties(Jwt jwt, Google google) {

	/** HS256 signing secret, and the lifetimes of access and refresh tokens. */
	public record Jwt(String secret, long accessTokenHours, long refreshTokenDays) {

		public Duration accessLifetime() {
			return Duration.ofHours(accessTokenHours);
		}

		public Duration refreshLifetime() {
			return Duration.ofDays(refreshTokenDays);
		}
	}

	/** The OAuth client id that Google ID tokens must be issued for (the audience). */
	public record Google(String clientId) {
	}
}
