package com.ascorp.prepai.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mockStatic;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class OpaqueTokensTest {

	private static final String ABC_SHA256 = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad";

	private final OpaqueTokens tokens = new OpaqueTokens();

	@Test
	void newTokensAreUniqueAndUrlSafe() {
		String first = tokens.newToken();

		assertThat(first).isNotEqualTo(tokens.newToken()).matches("[A-Za-z0-9_-]{43}");
	}

	@Test
	void hashIsSha256InHexAndStable() {
		assertThat(tokens.hash("abc")).isEqualTo(ABC_SHA256);
	}

	@Test
	void hashFailsLoudlyWhenTheJdkHasNoSha256() {
		try (MockedStatic<MessageDigest> digests = mockStatic(MessageDigest.class)) {
			digests.when(() -> MessageDigest.getInstance("SHA-256"))
					.thenThrow(new NoSuchAlgorithmException("missing"));

			assertThatThrownBy(() -> tokens.hash("abc")).isInstanceOf(IllegalStateException.class);
		}
	}
}
