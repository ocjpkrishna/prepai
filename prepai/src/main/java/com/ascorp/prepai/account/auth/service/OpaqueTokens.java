package com.ascorp.prepai.account.auth.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Component;

/** Random tokens that go to the student, and the SHA-256 hashes that go to the database. */
@Component
public class OpaqueTokens {

	private static final int TOKEN_BYTES = 32;
	private static final String HASH_ALGORITHM = "SHA-256";

	private final SecureRandom random = new SecureRandom();

	public String newToken() {
		byte[] bytes = new byte[TOKEN_BYTES];
		random.nextBytes(bytes);
		return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
	}

	/** The hash in hex. Only this is stored, so a leaked table does not reveal usable tokens. */
	public String hash(String token) {
		try {
			byte[] digest = MessageDigest.getInstance(HASH_ALGORITHM).digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException exception) {
			throw new IllegalStateException(HASH_ALGORITHM + " is required by every Java runtime", exception);
		}
	}
}
