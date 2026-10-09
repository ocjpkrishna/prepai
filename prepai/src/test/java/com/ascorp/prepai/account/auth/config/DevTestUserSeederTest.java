package com.ascorp.prepai.account.auth.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class DevTestUserSeederTest {

	@Mock
	private UserRepository users;

	@Mock
	private PasswordEncoder passwordEncoder;

	private DevTestUserSeeder seeder(String password) {
		return new DevTestUserSeeder(users, passwordEncoder,
				Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneOffset.UTC),
				new DevTestUserProperties("Test@PrepAI.local", password));
	}

	@Test
	void aVerifiedProPlusStudentIsCreatedWhenThePasswordIsSet() {
		when(users.findByEmail("test@prepai.local")).thenReturn(Optional.empty());
		when(passwordEncoder.encode("a-test-password")).thenReturn("hashed");

		seeder("a-test-password").run(null);

		ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
		verify(users).save(saved.capture());
		assertThat(saved.getValue().getEmail()).isEqualTo("test@prepai.local");
		assertThat(saved.getValue().getPasswordHash()).isEqualTo("hashed");
		assertThat(saved.getValue().isEmailVerified()).isTrue();
		assertThat(saved.getValue().getPlan()).isEqualTo(Plan.PRO_PLUS);
	}

	@Test
	void nothingIsCreatedWithoutAPassword() {
		seeder("").run(null);

		verify(users, never()).save(any());
	}

	@Test
	void anExistingTestUserIsLeftAlone() {
		when(users.findByEmail("test@prepai.local")).thenReturn(Optional.of(User.newEmailAccount(
				java.util.UUID.randomUUID(), "test@prepai.local", "x", "T")));

		seeder("a-test-password").run(null);

		verify(users, never()).save(any());
	}
}
