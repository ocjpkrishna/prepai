package com.ascorp.prepai.account.auth.service;

import com.ascorp.prepai.account.auth.model.dto.GoogleLoginRequest;
import com.ascorp.prepai.account.auth.model.dto.LoginRequest;
import com.ascorp.prepai.account.auth.model.dto.RefreshRequest;
import com.ascorp.prepai.account.auth.model.dto.RegisterRequest;
import com.ascorp.prepai.account.auth.model.dto.TokenResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The entry point of the account API (spec 4.1): each method joins an account step with the token step. */
@Service
@RequiredArgsConstructor
public class AuthService {

	private final PasswordAccountService passwordAccounts;
	private final GoogleSignInService googleSignIn;
	private final EmailVerificationService emailVerification;
	private final TokenService tokens;

	@Transactional
	public TokenResponse register(RegisterRequest request) {
		return tokens.issue(passwordAccounts.register(request).getId());
	}

	@Transactional
	public TokenResponse login(LoginRequest request) {
		return tokens.issue(passwordAccounts.authenticate(request).getId());
	}

	public TokenResponse refresh(RefreshRequest request) {
		return tokens.rotate(request.refreshToken());
	}

	@Transactional
	public TokenResponse loginWithGoogle(GoogleLoginRequest request) {
		return tokens.issue(googleSignIn.signIn(request).getId());
	}

	public void verifyEmail(String token) {
		emailVerification.verify(token);
	}
}
