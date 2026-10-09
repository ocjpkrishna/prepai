package com.ascorp.prepai.account.auth.service;

import com.ascorp.prepai.account.auth.config.AuthProperties;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimNames;
import org.springframework.security.oauth2.jwt.JwtClaimValidator;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtTimestampValidator;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.stereotype.Component;

/**
 * Checks a Google ID token against Google's published keys, its issuer, its audience (our client id) and its
 * timestamps. The frontend sends the token; the backend never runs a server-side OAuth redirect (spec 4.1).
 */
@Component
public class GoogleTokenVerifier {

	private static final String CERTS_URI = "https://www.googleapis.com/oauth2/v3/certs";
	private static final List<String> ISSUERS = List.of("https://accounts.google.com", "accounts.google.com");
	private static final String UNVERIFIED_MESSAGE = "That Google account has no verified email address.";

	private final JwtDecoder decoder;

	@Autowired
	public GoogleTokenVerifier(AuthProperties properties) {
		this(googleDecoder(properties.google().clientId()));
	}

	GoogleTokenVerifier(JwtDecoder decoder) {
		this.decoder = decoder;
	}

	/** The Google identity behind a valid ID token, or UNAUTHENTICATED when the token does not pass the checks. */
	public GoogleIdentity verify(String idToken) {
		try {
			return identityOf(decoder.decode(idToken));
		} catch (JwtException exception) {
			throw new ApiException(ErrorCode.UNAUTHENTICATED, "That Google sign-in could not be verified.");
		}
	}

	/** A Google identity: the stable subject, the email and the display name. */
	public record GoogleIdentity(String subject, String email, String name) {
	}

	private GoogleIdentity identityOf(Jwt jwt) {
		String email = jwt.getClaimAsString("email");
		if (email == null || !Boolean.TRUE.equals(jwt.getClaimAsBoolean("email_verified"))) {
			throw new ApiException(ErrorCode.UNAUTHENTICATED, UNVERIFIED_MESSAGE);
		}
		return new GoogleIdentity(jwt.getSubject(), email, jwt.getClaimAsString("name"));
	}

	private static JwtDecoder googleDecoder(String clientId) {
		NimbusJwtDecoder decoder = NimbusJwtDecoder.withJwkSetUri(CERTS_URI).build();
		decoder.setJwtValidator(googleValidator(clientId));
		return decoder;
	}

	static OAuth2TokenValidator<Jwt> googleValidator(String clientId) {
		return new DelegatingOAuth2TokenValidator<>(
				new JwtTimestampValidator(),
				new JwtClaimValidator<String>(JwtClaimNames.ISS, ISSUERS::contains),
				new JwtClaimValidator<List<String>>(
						JwtClaimNames.AUD, audience -> audience != null && audience.contains(clientId)));
	}
}
