package com.ascorp.prepai.account.auth.service;

import com.ascorp.prepai.account.auth.model.dto.LoginRequest;
import com.ascorp.prepai.account.auth.model.dto.RegisterRequest;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.privacy.service.ConsentService;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sign-up and password log-in. Passwords are BCrypt-hashed and never stored or logged in clear. */
@Service
@RequiredArgsConstructor
public class PasswordAccountService {

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final EmailVerificationService emailVerification;
	private final ConsentService consent;
	private final Supplier<UUID> ids;

	@Transactional
	public User register(RegisterRequest request) {
		User user = saveNewUser(User.newEmailAccount(ids.get(), User.normaliseEmail(request.email()),
				passwordEncoder.encode(request.password()), request.name()));
		consent.recordSignUp(user, request);
		emailVerification.sendVerification(user);
		return user;
	}

	/** An account that asked for deletion cannot log in, although its data is only purged after 30 days. */
	@Transactional
	public User authenticate(LoginRequest request) {
		return users.findByEmail(User.normaliseEmail(request.email()))
				.filter(candidate -> !candidate.isDeletionRequested())
				.filter(candidate -> passwordMatches(candidate, request.password()))
				.orElseThrow(PasswordAccountService::invalidCredentials);
	}

	/** The unique email constraint decides, so two concurrent sign-ups cannot both succeed. */
	private User saveNewUser(User user) {
		try {
			return users.saveAndFlush(user);
		} catch (DataIntegrityViolationException exception) {
			throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS,
					"An account with this email already exists. Log in, or verify your email first.");
		}
	}

	private boolean passwordMatches(User user, String rawPassword) {
		return user.getPasswordHash() != null && passwordEncoder.matches(rawPassword, user.getPasswordHash());
	}

	private static ApiException invalidCredentials() {
		return new ApiException(ErrorCode.UNAUTHENTICATED, "The email or password is incorrect.");
	}
}
