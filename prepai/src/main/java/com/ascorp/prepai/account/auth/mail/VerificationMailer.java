package com.ascorp.prepai.account.auth.mail;

/**
 * Sends the email-verification and guardian-consent links. The SMTP implementation waits for the provider
 * (BUILD-DECISIONS.md, Needs the user); tests replace it. Implementations must never log the recipient or the token.
 */
public interface VerificationMailer {

	void sendVerification(String recipient, String token);

	void sendGuardianConsent(String recipient, String token);
}
