package com.ascorp.prepai.account.privacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.RefreshTokenRepository;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.repository.VerificationTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountDeletionServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");

	@Mock
	private UserRepository users;

	@Mock
	private RefreshTokenRepository refreshTokens;

	@Mock
	private VerificationTokenRepository verificationTokens;

	@Mock
	private PersonalDataService personalData;

	private AccountDeletionService service;
	private User student;

	@BeforeEach
	void setUp() {
		service = new AccountDeletionService(users, refreshTokens, verificationTokens, personalData,
				Clock.fixed(NOW, ZoneOffset.UTC));
		student = User.newEmailAccount(USER_ID, "asha@example.com", "hash", "Asha");
	}

	@Test
	void requestingDeletionMarksTheAccountAndEndsAllSessions() {
		when(users.findById(USER_ID)).thenReturn(Optional.of(student));

		service.requestDeletion(USER_ID);

		assertThat(student.getDeletionRequestedAt()).isEqualTo(NOW);
		verify(refreshTokens).revokeAllForUser(USER_ID, NOW);
	}

	@Test
	void thePurgeErasesEveryModuleAndAnonymisesTheRowInThatOrder() {
		student.recordSignUpConsent(false, null, NOW, "2026-10");
		student.requestDeletion(NOW.minusSeconds(31L * 24 * 60 * 60));
		when(users.findByDeletionRequestedAtBeforeAndPurgedAtIsNull(NOW.minusSeconds(30L * 24 * 60 * 60)))
				.thenReturn(List.of(student));

		int purged = service.purgeDueAccounts();

		InOrder order = inOrder(personalData, refreshTokens, verificationTokens);
		order.verify(personalData).erase(USER_ID);
		order.verify(refreshTokens).deleteByUserId(USER_ID);
		order.verify(verificationTokens).deleteByUserId(USER_ID);
		assertThat(purged).isEqualTo(1);
		assertThat(student.getPurgedAt()).isEqualTo(NOW);
		assertThat(student.getEmail()).isEqualTo("purged-" + USER_ID + "@deleted.invalid");
		assertThat(student.getPasswordHash()).isNull();
	}

	@Test
	void noAccountIsPurgedBeforeItsThirtyDays() {
		when(users.findByDeletionRequestedAtBeforeAndPurgedAtIsNull(any(Instant.class))).thenReturn(List.of());

		assertThat(service.purgeDueAccounts()).isZero();
		verify(personalData, never()).erase(any(UUID.class));
	}
}
