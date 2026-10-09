package com.ascorp.prepai.account.auth.mail;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.ascorp.prepai.common.config.AppProperties;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

@ExtendWith(MockitoExtension.class)
class SmtpVerificationMailerTest {

	@Mock
	private JavaMailSender sender;

	private SmtpVerificationMailer mailer;

	@BeforeEach
	void setUp() {
		mailer = new SmtpVerificationMailer(sender,
				new AppProperties("https://prepai.example", List.of(), "no-reply@prepai.example"));
	}

	@Test
	void theVerificationLinkCarriesTheTokenToTheRecipient() {

		mailer.sendVerification("student@example.com", "abc123");

		ArgumentCaptor<SimpleMailMessage> sent = ArgumentCaptor.forClass(SimpleMailMessage.class);
		verify(sender).send(sent.capture());
		assertThat(sent.getValue().getTo()).containsExactly("student@example.com");
		assertThat(sent.getValue().getText()).contains("https://prepai.example/api/v1/auth/verify-email?token=abc123");
	}

	@Test
	void aMailServerOutageBecomesAnEmailUnavailableError() {
		doThrow(new MailSendException("connection refused")).when(sender).send(any(SimpleMailMessage.class));

		assertThatThrownBy(() -> mailer.sendVerification("student@example.com", "abc123"))
				.isInstanceOf(ApiException.class)
				.extracting(failure -> ((ApiException) failure).getCode())
				.isEqualTo(ErrorCode.EMAIL_UNAVAILABLE);
	}
}
