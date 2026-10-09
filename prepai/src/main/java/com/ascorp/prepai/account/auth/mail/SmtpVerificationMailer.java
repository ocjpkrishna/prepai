package com.ascorp.prepai.account.auth.mail;

import com.ascorp.prepai.common.config.AppProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/** Sends the verification link through the SMTP server configured with spring.mail.* (spec 2.7). */
@Component
@RequiredArgsConstructor
public class SmtpVerificationMailer implements VerificationMailer {

	private static final String VERIFY_PATH = "/api/v1/auth/verify-email?token=";
	private static final String GUARDIAN_PATH = "/api/v1/auth/guardian-consent/confirm?token=";

	private final JavaMailSender sender;
	private final AppProperties app;

	@Override
	public void sendVerification(String recipient, String token) {
		send(recipient, "Verify your PrepAI email",
				"Confirm your email to start learning with PrepAI:\n\n" + app.baseUrl() + VERIFY_PATH + token);
	}

	@Override
	public void sendGuardianConsent(String recipient, String token) {
		send(recipient, "Consent for a PrepAI student account",
				"A student has asked to use PrepAI with this email as their guardian. Confirm your consent here:\n\n"
						+ app.baseUrl() + GUARDIAN_PATH + token);
	}

	private void send(String recipient, String subject, String text) {
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(app.mailFrom());
		message.setTo(recipient);
		message.setSubject(subject);
		message.setText(text);
		sender.send(message);
	}
}
