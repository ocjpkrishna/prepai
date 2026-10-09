package com.ascorp.prepai.account.user.model.dto;

import com.ascorp.prepai.common.model.enums.Language;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Preferences (spec 4.3). The voice speed range is a build decision (BUILD-DECISIONS.md). */
public record PreferencesRequest(
		@NotNull Language language,
		@NotNull @DecimalMin("0.5") @DecimalMax("2.0") Double voiceSpeed,
		@NotNull @Pattern(regexp = "light|dark") String theme) {
}
