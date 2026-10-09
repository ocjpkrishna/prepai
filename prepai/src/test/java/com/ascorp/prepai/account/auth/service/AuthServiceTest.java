package com.ascorp.prepai.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.model.dto.GoogleLoginRequest;
import com.ascorp.prepai.account.auth.model.dto.LoginRequest;
import com.ascorp.prepai.account.auth.model.dto.RefreshRequest;
import com.ascorp.prepai.account.auth.model.dto.RegisterRequest;
import com.ascorp.prepai.account.auth.model.dto.TokenResponse;
import com.ascorp.prepai.account.auth.model.entity.User;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");
	private static final TokenResponse TOKENS = new TokenResponse("access", "refresh", 3600);
	private static final User ACCOUNT = User.newEmailAccount(USER_ID, "asha@example.com", "hash", "Asha");

	@Mock
	private PasswordAccountService passwordAccounts;

	@Mock
	private GoogleSignInService googleSignIn;

	@Mock
	private EmailVerificationService emailVerification;

	@Mock
	private TokenService tokens;

	@Test
	void registerIssuesTokensForTheNewAccount() {
		RegisterRequest request = new RegisterRequest("asha@example.com", "correct horse", "Asha",
				LocalDate.of(1990, 1, 1), true, null);
		when(passwordAccounts.register(request)).thenReturn(ACCOUNT);
		when(tokens.issue(USER_ID)).thenReturn(TOKENS);

		assertThat(newService().register(request)).isEqualTo(TOKENS);
	}

	@Test
	void loginIssuesTokensForTheAuthenticatedAccount() {
		LoginRequest request = new LoginRequest("asha@example.com", "correct horse");
		when(passwordAccounts.authenticate(request)).thenReturn(ACCOUNT);
		when(tokens.issue(USER_ID)).thenReturn(TOKENS);

		assertThat(newService().login(request)).isEqualTo(TOKENS);
	}

	@Test
	void refreshDelegatesToTheRotationOfTheTokenService() {
		when(tokens.rotate("refresh")).thenReturn(TOKENS);

		assertThat(newService().refresh(new RefreshRequest("refresh"))).isEqualTo(TOKENS);
	}

	@Test
	void googleSignInIssuesTokensForTheGoogleAccount() {
		GoogleLoginRequest request = new GoogleLoginRequest("id-token");
		when(googleSignIn.signIn(request)).thenReturn(ACCOUNT);
		when(tokens.issue(USER_ID)).thenReturn(TOKENS);

		assertThat(newService().loginWithGoogle(request)).isEqualTo(TOKENS);
	}

	@Test
	void verifyEmailDelegatesToTheVerificationService() {
		newService().verifyEmail("link-token");

		verify(emailVerification).verify("link-token");
	}

	private AuthService newService() {
		return new AuthService(passwordAccounts, googleSignIn, emailVerification, tokens);
	}
}
