package com.ascorp.prepai.account.auth.config;

import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Local development only: makes sure one verified Pro+ student exists, so the app can be tried without an email
 * server. The password comes from `DEV_TEST_PASSWORD`; nothing is stored in the repository.
 */
@Slf4j
@Component
@Profile("local")
@RequiredArgsConstructor
public class DevTestUserSeeder implements ApplicationRunner {

	private static final String POLICY_VERSION = "dev";

	private final UserRepository users;
	private final PasswordEncoder passwordEncoder;
	private final Clock clock;
	private final DevTestUserProperties settings;

	@Override
	public void run(ApplicationArguments args) {
		String email = User.normaliseEmail(settings.email());
		if (settings.password() == null || settings.password().isBlank()) {
			log.info("No test user: set DEV_TEST_PASSWORD to create {}", email);
			return;
		}
		if (users.findByEmail(email).isPresent()) {
			log.info("Test user {} already exists", email);
			return;
		}
		users.save(newTestUser(email));
		log.info("Created test user {} (verified, Pro+)", email);
	}

	private User newTestUser(String email) {
		Instant now = clock.instant();
		User student = User.newEmailAccount(UUID.randomUUID(), email,
				passwordEncoder.encode(settings.password()), "Test Student");
		student.markEmailVerified();
		student.recordSignUpConsent(false, null, now, POLICY_VERSION);
		student.changePlan(Plan.PRO_PLUS, null);
		return student;
	}
}
