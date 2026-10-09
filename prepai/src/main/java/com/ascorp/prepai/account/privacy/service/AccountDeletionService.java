package com.ascorp.prepai.account.privacy.service;

import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.RefreshTokenRepository;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.repository.VerificationTokenRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Account deletion (spec 2.7): a soft delete that ends every session at once, then a purge within 30 days. */
@Service
@RequiredArgsConstructor
public class AccountDeletionService {

	private static final Duration PURGE_AFTER = Duration.ofDays(30);

	private final UserRepository users;
	private final RefreshTokenRepository refreshTokens;
	private final VerificationTokenRepository verificationTokens;
	private final PersonalDataService personalData;
	private final Clock clock;

	@Transactional
	public void requestDeletion(UUID userId) {
		User student = users.findById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "We couldn't find this account."));
		Instant now = clock.instant();
		student.requestDeletion(now);
		refreshTokens.revokeAllForUser(userId, now);
	}

	/** Purges every account whose 30 days have passed. Returns how many were purged. */
	@Transactional
	public int purgeDueAccounts() {
		List<User> due = users.findByDeletionRequestedAtBeforeAndPurgedAtIsNull(clock.instant().minus(PURGE_AFTER));
		due.forEach(this::purge);
		return due.size();
	}

	private void purge(User student) {
		personalData.erase(student.getId());
		refreshTokens.deleteByUserId(student.getId());
		verificationTokens.deleteByUserId(student.getId());
		student.anonymise(clock.instant());
	}
}
