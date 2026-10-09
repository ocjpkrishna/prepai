package com.ascorp.prepai.account.privacy.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ascorp.prepai.account.auth.model.dto.RegisterRequest;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConsentServiceTest {

	private static final UUID STUDENT_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final Instant NOW = Instant.parse("2026-10-08T00:00:00Z");
	private static final String GUARDIAN = "parent@example.com";

	@Mock
	private GuardianConsentService guardianConsent;

	private ConsentService service;
	private User student;

	@BeforeEach
	void setUp() {
		service = new ConsentService(guardianConsent, Clock.fixed(NOW, ZoneOffset.UTC));
		student = User.newEmailAccount(STUDENT_ID, "asha@example.com", "hash", "Asha");
	}

	@Test
	void anAdultSignUpRecordsTermsAndSendsNoGuardianLink() {
		service.recordSignUp(student, request(LocalDate.of(1990, 1, 1), null));

		assertThat(student.isMinor()).isFalse();
		assertThat(student.getGuardianEmail()).isNull();
		assertThat(student.getTermsAcceptedAt()).isEqualTo(NOW);
		assertThat(student.getPrivacyPolicyVersion()).isEqualTo(ConsentService.POLICY_VERSION);
		verifyNoInteractions(guardianConsent);
	}

	@Test
	void aMinorNeedsAGuardianEmail() {
		assertThatThrownBy(() -> service.recordSignUp(student, request(birthDayOfAge(15), " ")))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
		verifyNoInteractions(guardianConsent);
	}

	@Test
	void aMinorIsFlaggedAndTheGuardianGetsAConsentLink() {
		service.recordSignUp(student, request(birthDayOfAge(15), " Parent@Example.COM "));

		assertThat(student.isMinor()).isTrue();
		assertThat(student.getGuardianEmail()).isEqualTo(GUARDIAN);
		verify(guardianConsent).sendConsentLink(STUDENT_ID, GUARDIAN);
	}

	private static RegisterRequest request(LocalDate birthDate, String guardianEmail) {
		return new RegisterRequest("asha@example.com", "correct horse", "Asha", birthDate, true, guardianEmail);
	}

	private static LocalDate birthDayOfAge(int years) {
		return LocalDate.of(NOW.atZone(ZoneOffset.UTC).getYear() - years, 1, 1);
	}
}
