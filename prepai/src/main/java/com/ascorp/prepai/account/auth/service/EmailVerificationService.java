package com.ascorp.prepai.account.auth.service;

import com.ascorp.prepai.account.auth.mail.VerificationMailer;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.model.entity.VerificationToken;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.repository.VerificationTokenRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sends and confirms the one-time link that proves the student owns the email address (spec 4.1, 2.7). */
@Service
@RequiredArgsConstructor
public class EmailVerificationService {

	private static final int LINK_HOURS = 24;

	private final VerificationTokenRepository verificationTokens;
	private final UserRepository users;
	private final OpaqueTokens opaqueTokens;
	private final VerificationMailer mailer;
	private final Clock clock;
	private final Supplier<UUID> ids;

	@Transactional
	public void sendVerification(User user) {
		String token = opaqueTokens.newToken();
		verificationTokens.save(VerificationToken.builder()
				.id(ids.get())
				.userId(user.getId())
				.type(VerificationToken.Type.EMAIL)
				.tokenHash(opaqueTokens.hash(token))
				.expiresAt(clock.instant().plus(Duration.ofHours(LINK_HOURS)))
				.build());
		mailer.sendVerification(user.getEmail(), token);
	}

	@Transactional
	public void verify(String token) {
		VerificationToken link = verificationTokens
				.findByTokenHashAndType(opaqueTokens.hash(token), VerificationToken.Type.EMAIL)
				.filter(candidate -> candidate.isUsable(clock.instant()))
				.orElseThrow(EmailVerificationService::invalidLink);
		link.markUsed(clock.instant());
		User user = users.findById(link.getUserId()).orElseThrow(EmailVerificationService::invalidLink);
		user.markEmailVerified();
	}

	private static ApiException invalidLink() {
		return new ApiException(ErrorCode.VALIDATION_FAILED, "This link is invalid or has expired.");
	}
}
