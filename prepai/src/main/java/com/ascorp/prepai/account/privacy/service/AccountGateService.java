package com.ascorp.prepai.account.privacy.service;

import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Decides whether a student may generate lessons (spec 2.7). Lesson asks this before it reserves a session. */
@Service
@RequiredArgsConstructor
public class AccountGateService {

	private final UserRepository users;

	@Transactional(readOnly = true)
	public void requireLessonAccess(UUID userId) {
		User student = users.findById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "We couldn't find this account."));
		if (student.isDeletionRequested()) {
			throw new ApiException(ErrorCode.FORBIDDEN, "This account is being deleted.");
		}
		if (!student.isEmailVerified()) {
			throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED, "Please verify your email to continue.");
		}
		if (student.isMinor() && !student.isGuardianConsented()) {
			throw new ApiException(ErrorCode.CONSENT_REQUIRED, "Waiting for guardian consent.");
		}
	}
}
