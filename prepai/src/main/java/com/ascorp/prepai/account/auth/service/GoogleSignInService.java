package com.ascorp.prepai.account.auth.service;

import com.ascorp.prepai.account.auth.model.dto.GoogleLoginRequest;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.service.GoogleTokenVerifier.GoogleIdentity;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Google sign-in: finds or creates the account behind a verified Google ID token (spec 4.1). */
@Service
@RequiredArgsConstructor
public class GoogleSignInService {

	private final UserRepository users;
	private final GoogleTokenVerifier googleTokenVerifier;
	private final Supplier<UUID> ids;

	@Transactional
	public User signIn(GoogleLoginRequest request) {
		GoogleIdentity identity = googleTokenVerifier.verify(request.idToken());
		User user = users.findByGoogleId(identity.subject()).orElseGet(() -> attachGoogle(identity));
		return rejectDeleted(user);
	}

	/** An account that asked for deletion cannot sign in again, even through Google (spec 2.7). */
	private static User rejectDeleted(User user) {
		if (user.isDeletionRequested()) {
			throw new ApiException(ErrorCode.UNAUTHENTICATED, "This account is no longer available.");
		}
		return user;
	}

	/** A Google account joins an existing one only when that account's email is already proven. */
	private User attachGoogle(GoogleIdentity identity) {
		String email = User.normaliseEmail(identity.email());
		return users.findByEmail(email)
				.map(existing -> linkExisting(existing, identity))
				.orElseGet(() -> users.saveAndFlush(
						User.newGoogleAccount(ids.get(), email, identity.name(), identity.subject())));
	}

	private static User linkExisting(User existing, GoogleIdentity identity) {
		if (!existing.isEmailVerified()) {
			throw new ApiException(ErrorCode.EMAIL_ALREADY_EXISTS,
					"An account with this email already exists. Log in, or verify your email first.");
		}
		existing.linkGoogle(identity.subject());
		return existing;
	}
}
