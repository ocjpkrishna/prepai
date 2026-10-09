package com.ascorp.prepai.account.privacy.service;

import com.ascorp.prepai.account.auth.mail.VerificationMailer;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.model.entity.VerificationToken;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.repository.VerificationTokenRepository;
import com.ascorp.prepai.account.auth.service.OpaqueTokens;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sends the guardian's consent link and records the guardian's answer (spec 2.7, 4.1). */
@Service
@RequiredArgsConstructor
public class GuardianConsentService {

	private static final Duration LINK_LIFETIME = Duration.ofDays(7);

	private final VerificationTokenRepository verificationTokens;
	private final UserRepository users;
	private final OpaqueTokens opaqueTokens;
	private final VerificationMailer mailer;
	private final Clock clock;
	private final Supplier<UUID> ids;

	@Transactional
	public void sendConsentLink(UUID studentId, String guardianEmail) {
		String token = opaqueTokens.newToken();
		verificationTokens.save(VerificationToken.builder()
				.id(ids.get())
				.userId(studentId)
				.type(VerificationToken.Type.GUARDIAN)
				.tokenHash(opaqueTokens.hash(token))
				.expiresAt(clock.instant().plus(LINK_LIFETIME))
				.build());
		mailer.sendGuardianConsent(guardianEmail, token);
	}

	@Transactional
	public void confirm(String token) {
		VerificationToken link = verificationTokens
				.findByTokenHashAndType(opaqueTokens.hash(token), VerificationToken.Type.GUARDIAN)
				.filter(candidate -> candidate.isUsable(clock.instant()))
				.orElseThrow(GuardianConsentService::invalidLink);
		link.markUsed(clock.instant());
		User student = users.findById(link.getUserId()).orElseThrow(GuardianConsentService::invalidLink);
		student.confirmGuardianConsent(clock.instant());
	}

	private static ApiException invalidLink() {
		return new ApiException(ErrorCode.VALIDATION_FAILED, "This link is invalid or has expired.");
	}
}
