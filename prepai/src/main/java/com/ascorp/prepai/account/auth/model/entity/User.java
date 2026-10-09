package com.ascorp.prepai.account.auth.model.entity;

import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Plan;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A student account (spec 5.1). Private to the account module: other modules get ids through its services. */
@Entity
@Table(name = "users")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class User {

	private static final double DEFAULT_VOICE_SPEED = 1.0;
	private static final String DEFAULT_THEME = "light";

	@Id
	private UUID id;

	@Column(nullable = false, unique = true)
	private String email;

	private String passwordHash;

	private String name;

	private String googleId;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Plan plan;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Language language;

	@Column(nullable = false)
	private boolean emailVerified;

	@Column(name = "is_minor", nullable = false)
	private boolean minor;

	private String guardianEmail;

	private Instant guardianConsentAt;

	private Instant termsAcceptedAt;

	private String privacyPolicyVersion;

	@Builder.Default
	@Column(nullable = false)
	private double voiceSpeed = DEFAULT_VOICE_SPEED;

	@Builder.Default
	@Column(nullable = false, length = 10)
	private String theme = DEFAULT_THEME;

	private Instant deletionRequestedAt;

	private Instant purgedAt;

	public void markEmailVerified() {
		this.emailVerified = true;
	}

	public void linkGoogle(String googleId) {
		this.googleId = googleId;
	}

	public void updatePreferences(Language preferredLanguage, double speed, String appTheme) {
		this.language = preferredLanguage;
		this.voiceSpeed = speed;
		this.theme = appTheme;
	}

	/** The age gate, terms and guardian details given at sign-up (spec 2.7). */
	public void recordSignUpConsent(boolean underage, String guardian, Instant termsAt, String policyVersion) {
		this.minor = underage;
		this.guardianEmail = guardian;
		this.termsAcceptedAt = termsAt;
		this.privacyPolicyVersion = policyVersion;
	}

	public void confirmGuardianConsent(Instant now) {
		this.guardianConsentAt = now;
	}

	/** Soft delete: the first request wins, so a repeated request keeps the original date for the 30-day purge. */
	public void requestDeletion(Instant now) {
		if (deletionRequestedAt == null) {
			deletionRequestedAt = now;
		}
	}

	/** Removes everything personal but keeps the row, so ids in other tables still point at something (spec 2.7). */
	public void anonymise(Instant now) {
		this.email = "purged-" + id + "@deleted.invalid";
		this.passwordHash = null;
		this.name = null;
		this.googleId = null;
		this.guardianEmail = null;
		this.purgedAt = now;
	}

	public boolean isDeletionRequested() {
		return deletionRequestedAt != null;
	}

	public boolean isGuardianConsented() {
		return guardianConsentAt != null;
	}

	/** A new account that signs up with email and password. The email still has to be verified. */
	public static User newEmailAccount(UUID id, String email, String passwordHash, String name) {
		return builder().id(id).email(email).passwordHash(passwordHash).name(name)
				.plan(Plan.FREE).language(Language.EN).emailVerified(false).build();
	}

	/** A new account that signs in with Google. Google has already verified the email. */
	public static User newGoogleAccount(UUID id, String email, String name, String googleId) {
		return builder().id(id).email(email).name(name).googleId(googleId)
				.plan(Plan.FREE).language(Language.EN).emailVerified(true).build();
	}

	/** The one form of an email address that is stored and compared. */
	public static String normaliseEmail(String email) {
		return email.trim().toLowerCase(Locale.ROOT);
	}
}
