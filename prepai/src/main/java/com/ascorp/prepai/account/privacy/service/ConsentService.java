package com.ascorp.prepai.account.privacy.service;

import com.ascorp.prepai.account.auth.model.dto.RegisterRequest;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** The age gate and terms at sign-up (spec 2.7). Under-18s need a guardian, who gets a consent link. */
@Service
@RequiredArgsConstructor
public class ConsentService {

	static final String POLICY_VERSION = "2026-10";
	private static final int ADULT_AGE = 18;

	private final GuardianConsentService guardianConsent;
	private final Clock clock;

	/** The birth date only sets the age flag and is not stored. */
	public void recordSignUp(User user, RegisterRequest request) {
		boolean underage = isUnderage(request.birthDate());
		String guardianEmail = underage ? requireGuardian(request.guardianEmail()) : null;
		user.recordSignUpConsent(underage, guardianEmail, clock.instant(), POLICY_VERSION);
		if (underage) {
			guardianConsent.sendConsentLink(user.getId(), guardianEmail);
		}
	}

	private boolean isUnderage(LocalDate birthDate) {
		return Period.between(birthDate, LocalDate.now(clock)).getYears() < ADULT_AGE;
	}

	private static String requireGuardian(String guardianEmail) {
		if (guardianEmail == null || guardianEmail.isBlank()) {
			throw new ApiException(ErrorCode.VALIDATION_FAILED, "A guardian's email is needed for students under 18.");
		}
		return User.normaliseEmail(guardianEmail);
	}
}
