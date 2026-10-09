package com.ascorp.prepai.account.privacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AccountGateServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");

	@Mock
	private UserRepository users;

	private AccountGateService gate;

	@BeforeEach
	void setUp() {
		gate = new AccountGateService(users);
	}

	@Test
	void aVerifiedAdultMayGenerateLessons() {
		givenStudent(User.newGoogleAccount(USER_ID, "asha@example.com", "Asha", "google-subject"));

		assertThatCode(() -> gate.requireLessonAccess(USER_ID)).doesNotThrowAnyException();
	}

	@Test
	void anUnverifiedEmailIsBlocked() {
		givenStudent(User.newEmailAccount(USER_ID, "asha@example.com", "hash", "Asha"));

		assertBlockedWith(ErrorCode.EMAIL_NOT_VERIFIED);
	}

	@Test
	void aMinorWithoutGuardianConsentIsBlocked() {
		User minor = User.newGoogleAccount(USER_ID, "asha@example.com", "Asha", "google-subject");
		minor.recordSignUpConsent(true, "parent@example.com", Instant.EPOCH, "2026-10");
		givenStudent(minor);

		assertBlockedWith(ErrorCode.CONSENT_REQUIRED);
	}

	@Test
	void aMinorWithGuardianConsentMayGenerateLessons() {
		User minor = User.newGoogleAccount(USER_ID, "asha@example.com", "Asha", "google-subject");
		minor.recordSignUpConsent(true, "parent@example.com", Instant.EPOCH, "2026-10");
		minor.confirmGuardianConsent(Instant.EPOCH);
		givenStudent(minor);

		assertThatCode(() -> gate.requireLessonAccess(USER_ID)).doesNotThrowAnyException();
	}

	@Test
	void anAccountBeingDeletedIsBlocked() {
		User deleting = User.newGoogleAccount(USER_ID, "asha@example.com", "Asha", "google-subject");
		deleting.requestDeletion(Instant.EPOCH);
		givenStudent(deleting);

		assertBlockedWith(ErrorCode.FORBIDDEN);
	}

	@Test
	void anUnknownAccountIsNotFound() {
		when(users.findById(USER_ID)).thenReturn(Optional.empty());

		assertBlockedWith(ErrorCode.NOT_FOUND);
	}

	private void givenStudent(User student) {
		when(users.findById(USER_ID)).thenReturn(Optional.of(student));
	}

	private void assertBlockedWith(ErrorCode code) {
		assertThatThrownBy(() -> gate.requireLessonAccess(USER_ID))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(code));
	}
}
