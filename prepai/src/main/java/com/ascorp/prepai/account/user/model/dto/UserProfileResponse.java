package com.ascorp.prepai.account.user.model.dto;

import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Plan;
import java.util.UUID;

/** The student's profile and preferences (spec 4.3). Never carries the password hash or the guardian's email. */
public record UserProfileResponse(
		UUID id,
		String email,
		String name,
		Plan plan,
		Language language,
		double voiceSpeed,
		String theme,
		boolean emailVerified,
		boolean minor) {
}
