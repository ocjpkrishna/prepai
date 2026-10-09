package com.ascorp.prepai.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.model.dto.GoogleLoginRequest;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.service.GoogleTokenVerifier.GoogleIdentity;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class GoogleSignInServiceTest {

	private static final UUID USER_ID = UUID.fromString("7b0e8d9e-2e3f-4b4c-9d5e-7f8091a2b3c4");
	private static final String EMAIL = "asha@example.com";
	private static final GoogleIdentity GOOGLE = new GoogleIdentity("google-subject", EMAIL, "Asha");
	private static final GoogleLoginRequest REQUEST = new GoogleLoginRequest("id-token");

	@Mock
	private UserRepository users;

	@Mock
	private GoogleTokenVerifier googleTokenVerifier;

	@Test
	void signInReturnsTheAccountAlreadyLinkedToTheGoogleSubject() {
		User linked = User.newGoogleAccount(USER_ID, EMAIL, "Asha", GOOGLE.subject());
		when(googleTokenVerifier.verify("id-token")).thenReturn(GOOGLE);
		when(users.findByGoogleId(GOOGLE.subject())).thenReturn(Optional.of(linked));

		assertThat(newService().signIn(REQUEST)).isSameAs(linked);
		verify(users, never()).findByEmail(any());
	}

	@Test
	void signInLinksAVerifiedExistingAccountWithTheSameEmail() {
		User existing = User.newEmailAccount(USER_ID, EMAIL, "hash", "Asha");
		existing.markEmailVerified();
		when(googleTokenVerifier.verify("id-token")).thenReturn(GOOGLE);
		when(users.findByGoogleId(GOOGLE.subject())).thenReturn(Optional.empty());
		when(users.findByEmail(EMAIL)).thenReturn(Optional.of(existing));

		assertThat(newService().signIn(REQUEST).getGoogleId()).isEqualTo(GOOGLE.subject());
	}

	@Test
	void signInRefusesToJoinAnUnverifiedAccountWithTheSameEmail() {
		when(googleTokenVerifier.verify("id-token")).thenReturn(GOOGLE);
		when(users.findByGoogleId(GOOGLE.subject())).thenReturn(Optional.empty());
		when(users.findByEmail(EMAIL))
				.thenReturn(Optional.of(User.newEmailAccount(USER_ID, EMAIL, "hash", "Asha")));

		assertThatThrownBy(() -> newService().signIn(REQUEST))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.EMAIL_ALREADY_EXISTS));
	}

	@Test
	void signInCreatesAVerifiedAccountForANewStudent() {
		when(googleTokenVerifier.verify("id-token")).thenReturn(GOOGLE);
		when(users.findByGoogleId(GOOGLE.subject())).thenReturn(Optional.empty());
		when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
		when(users.saveAndFlush(any(User.class))).thenAnswer(call -> call.getArgument(0));

		newService().signIn(REQUEST);

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(users).saveAndFlush(saved.capture());
		assertThat(saved.getValue().isEmailVerified()).isTrue();
		assertThat(saved.getValue().getGoogleId()).isEqualTo(GOOGLE.subject());
		assertThat(saved.getValue().getPasswordHash()).isNull();
	}

	private GoogleSignInService newService() {
		return new GoogleSignInService(users, googleTokenVerifier, () -> USER_ID);
	}
}
