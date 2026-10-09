package com.ascorp.prepai.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.mail.VerificationMailer;
import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.model.entity.VerificationToken;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.repository.VerificationTokenRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EmailVerificationServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
	private static final UUID USER_ID = UUID.fromString("4f7b5a6b-9b0e-4c1f-8a2b-4c5d6e7f8091");
	private static final UUID LINK_ID = UUID.fromString("5a8c6b7c-0c1f-4d2a-9b3c-5d6e7f809102");
	private static final String EMAIL = "asha@example.com";
	private static final String RAW_TOKEN = "raw-link-token";

	@Mock
	private VerificationTokenRepository verificationTokens;

	@Mock
	private UserRepository users;

	@Mock
	private VerificationMailer mailer;

	private EmailVerificationService service;
	private final OpaqueTokens opaqueTokens = new OpaqueTokens();

	@BeforeEach
	void setUp() {
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		service = new EmailVerificationService(verificationTokens, users, opaqueTokens, mailer, clock, () -> LINK_ID);
	}

	@Test
	void sendVerificationStoresTheHashAndMailsTheRawToken() {
		service.sendVerification(user(false));

		ArgumentCaptor<String> mailed = ArgumentCaptor.forClass(String.class);
		verify(mailer).sendVerification(eq(EMAIL), mailed.capture());
		ArgumentCaptor<VerificationToken> saved = ArgumentCaptor.forClass(VerificationToken.class);
		verify(verificationTokens).save(saved.capture());
		assertThat(saved.getValue().getTokenHash()).isEqualTo(opaqueTokens.hash(mailed.getValue()));
		assertThat(saved.getValue().getType()).isEqualTo(VerificationToken.Type.EMAIL);
		assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(1)));
	}

	@Test
	void verifyMarksTheLinkUsedAndTheUserVerified() {
		VerificationToken link = link(null, NOW.plus(Duration.ofDays(1)));
		User user = user(false);
		when(verificationTokens.findByTokenHashAndType(opaqueTokens.hash(RAW_TOKEN), VerificationToken.Type.EMAIL))
				.thenReturn(Optional.of(link));
		when(users.findById(USER_ID)).thenReturn(Optional.of(user));

		service.verify(RAW_TOKEN);

		assertThat(link.getUsedAt()).isEqualTo(NOW);
		assertThat(user.isEmailVerified()).isTrue();
	}

	@Test
	void verifyRejectsAUsedLink() {
		when(verificationTokens.findByTokenHashAndType(opaqueTokens.hash(RAW_TOKEN), VerificationToken.Type.EMAIL))
				.thenReturn(Optional.of(link(NOW.minus(Duration.ofMinutes(1)), NOW.plus(Duration.ofDays(1)))));

		assertRejected();
		verifyNoInteractions(users);
	}

	@Test
	void verifyRejectsAnUnknownToken() {
		when(verificationTokens.findByTokenHashAndType(opaqueTokens.hash(RAW_TOKEN), VerificationToken.Type.EMAIL))
				.thenReturn(Optional.empty());

		assertRejected();
	}

	@Test
	void verifyRejectsALinkWhoseAccountNoLongerExists() {
		when(verificationTokens.findByTokenHashAndType(opaqueTokens.hash(RAW_TOKEN), VerificationToken.Type.EMAIL))
				.thenReturn(Optional.of(link(null, NOW.plus(Duration.ofDays(1)))));
		when(users.findById(USER_ID)).thenReturn(Optional.empty());

		assertRejected();
	}

	private void assertRejected() {
		assertThatThrownBy(() -> service.verify(RAW_TOKEN))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED));
	}

	private VerificationToken link(Instant usedAt, Instant expiresAt) {
		return VerificationToken.builder()
				.id(LINK_ID)
				.userId(USER_ID)
				.type(VerificationToken.Type.EMAIL)
				.tokenHash(opaqueTokens.hash(RAW_TOKEN))
				.expiresAt(expiresAt)
				.usedAt(usedAt)
				.build();
	}

	private static User user(boolean verified) {
		return User.builder()
				.id(USER_ID)
				.email(EMAIL)
				.plan(Plan.FREE)
				.language(Language.EN)
				.emailVerified(verified)
				.build();
	}
}
