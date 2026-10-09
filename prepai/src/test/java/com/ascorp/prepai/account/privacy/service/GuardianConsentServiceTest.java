package com.ascorp.prepai.account.privacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.mail.VerificationMailer;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.model.entity.VerificationToken;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.repository.VerificationTokenRepository;
import com.ascorp.prepai.account.auth.service.OpaqueTokens;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GuardianConsentServiceTest {

	private static final UUID STUDENT_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final UUID TOKEN_ID = UUID.fromString("0f1e2d3c-4b5a-4968-8778-6a5b4c3d2e1f");
	private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
	private static final String GUARDIAN = "parent@example.com";
	private static final String TOKEN = "raw-token";
	private static final String HASH = "hashed-token";

	@Mock
	private VerificationTokenRepository verificationTokens;

	@Mock
	private UserRepository users;

	@Mock
	private OpaqueTokens opaqueTokens;

	@Mock
	private VerificationMailer mailer;

	private GuardianConsentService service;
	private User student;

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		service = new GuardianConsentService(verificationTokens, users, opaqueTokens, mailer, clock, () -> TOKEN_ID);
		student = User.newEmailAccount(STUDENT_ID, "asha@example.com", "hash", "Asha");
	}

	@Test
	void theConsentLinkIsStoredAsAHashAndMailedToTheGuardian() {
		when(opaqueTokens.newToken()).thenReturn(TOKEN);
		when(opaqueTokens.hash(TOKEN)).thenReturn(HASH);

		service.sendConsentLink(STUDENT_ID, GUARDIAN);

		ArgumentCaptor<VerificationToken> saved = ArgumentCaptor.forClass(VerificationToken.class);
		verify(verificationTokens).save(saved.capture());
		assertThat(saved.getValue().getType()).isEqualTo(VerificationToken.Type.GUARDIAN);
		assertThat(saved.getValue().getTokenHash()).isEqualTo(HASH);
		verify(mailer).sendGuardianConsent(GUARDIAN, TOKEN);
	}

	@Test
	void aValidLinkConfirmsConsentOnce() {
		VerificationToken link = guardianLink(NOW.plusSeconds(60));
		when(opaqueTokens.hash(TOKEN)).thenReturn(HASH);
		when(verificationTokens.findByTokenHashAndType(HASH, VerificationToken.Type.GUARDIAN))
				.thenReturn(Optional.of(link));
		when(users.findById(STUDENT_ID)).thenReturn(Optional.of(student));

		service.confirm(TOKEN);

		assertThat(student.isGuardianConsented()).isTrue();
		assertThat(link.isUsable(NOW)).isFalse();
	}

	@Test
	void anExpiredLinkIsRejected() {
		when(opaqueTokens.hash(TOKEN)).thenReturn(HASH);
		when(verificationTokens.findByTokenHashAndType(HASH, VerificationToken.Type.GUARDIAN))
				.thenReturn(Optional.of(guardianLink(NOW.minusSeconds(1))));

		assertThatThrownBy(() -> service.confirm(TOKEN))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
		assertThat(student.isGuardianConsented()).isFalse();
	}

	private VerificationToken guardianLink(Instant expiresAt) {
		return VerificationToken.builder()
				.id(TOKEN_ID)
				.userId(STUDENT_ID)
				.type(VerificationToken.Type.GUARDIAN)
				.tokenHash(HASH)
				.expiresAt(expiresAt)
				.build();
	}
}
