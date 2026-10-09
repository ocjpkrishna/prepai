package com.ascorp.prepai.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.model.dto.LoginRequest;
import com.ascorp.prepai.account.auth.model.dto.RegisterRequest;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.privacy.service.ConsentService;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordAccountServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final String EMAIL = "asha@example.com";
	private static final String PASSWORD = "correct horse";
	private static final String HASH = "bcrypt-hash";
	private static final LocalDate BIRTH_DATE = LocalDate.of(1990, 1, 1);

	@Mock
	private UserRepository users;

	@Mock
	private PasswordEncoder passwordEncoder;

	@Mock
	private EmailVerificationService emailVerification;

	@Mock
	private ConsentService consent;

	private PasswordAccountService service;

	@Test
	void registerCreatesAnUnverifiedFreeAccountRecordsConsentAndSendsTheLink() {
		service = newService();
		RegisterRequest request = new RegisterRequest(" Asha@Example.COM ", PASSWORD, "Asha", BIRTH_DATE, true, null);
		when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
		when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

		User registered = service.register(request);

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(users).saveAndFlush(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo(EMAIL);
		assertThat(saved.getValue().getPasswordHash()).isEqualTo(HASH);
		assertThat(saved.getValue().getPlan()).isEqualTo(Plan.FREE);
		assertThat(saved.getValue().isEmailVerified()).isFalse();
		assertThat(registered).isSameAs(saved.getValue());
		verify(consent).recordSignUp(registered, request);
		verify(emailVerification).sendVerification(registered);
	}

	@Test
	void registerRejectsAnEmailThatAlreadyExists() {
		service = newService();
		when(passwordEncoder.encode(PASSWORD)).thenReturn(HASH);
		when(users.saveAndFlush(any(User.class))).thenThrow(new DataIntegrityViolationException("duplicate email"));

		assertThatThrownBy(() -> service.register(new RegisterRequest(EMAIL, PASSWORD, null, BIRTH_DATE, true, null)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
		verifyNoInteractions(emailVerification, consent);
	}

	@Test
	void authenticateRejectsAnAccountThatAskedForDeletion() {
		service = newService();
		User account = account(HASH);
		account.requestDeletion(Instant.parse("2026-10-01T00:00:00Z"));
		when(users.findByEmail(EMAIL)).thenReturn(Optional.of(account));

		assertThatThrownBy(() -> service.authenticate(new LoginRequest(EMAIL, PASSWORD)))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
		verifyNoInteractions(passwordEncoder);
	}

	@Test
	void authenticateReturnsTheAccountForTheRightPassword() {
		service = newService();
		when(users.findByEmail(EMAIL)).thenReturn(Optional.of(account(HASH)));
		when(passwordEncoder.matches(PASSWORD, HASH)).thenReturn(true);

		assertThat(service.authenticate(new LoginRequest(EMAIL, PASSWORD)).getId()).isEqualTo(USER_ID);
	}

	@Test
	void authenticateRejectsAWrongPassword() {
		service = newService();
		when(users.findByEmail(EMAIL)).thenReturn(Optional.of(account(HASH)));
		when(passwordEncoder.matches("wrong", HASH)).thenReturn(false);

		assertThatThrownBy(() -> service.authenticate(new LoginRequest(EMAIL, "wrong")))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
	}

	@Test
	void authenticateRejectsAGoogleOnlyAccountThatHasNoPassword() {
		service = newService();
		when(users.findByEmail(EMAIL)).thenReturn(Optional.of(account(null)));

		assertThatThrownBy(() -> service.authenticate(new LoginRequest(EMAIL, PASSWORD)))
				.isInstanceOf(ApiException.class);
	}

	private PasswordAccountService newService() {
		return new PasswordAccountService(users, passwordEncoder, emailVerification, consent, () -> USER_ID);
	}

	private static User account(String passwordHash) {
		return User.newEmailAccount(USER_ID, EMAIL, passwordHash, "Asha");
	}
}
