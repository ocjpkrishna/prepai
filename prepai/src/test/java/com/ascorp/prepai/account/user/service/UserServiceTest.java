package com.ascorp.prepai.account.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.privacy.service.AccountDeletionService;
import com.ascorp.prepai.account.privacy.service.PersonalDataService;
import com.ascorp.prepai.account.user.mapper.UserProfileMapper;
import com.ascorp.prepai.account.user.model.dto.PreferencesRequest;
import com.ascorp.prepai.account.user.model.dto.UserProfileResponse;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

	private static final UUID USER_ID = UUID.fromString("3e6a4f5a-8a9d-4b0e-9f1a-3b4c5d6e7f80");

	@Mock
	private UserRepository users;

	@Mock
	private UserProfileMapper profiles;

	@Mock
	private AccountDeletionService deletions;

	@Mock
	private PersonalDataService personalData;

	private UserService service;
	private User student;
	private UserProfileResponse profile;

	@BeforeEach
	void setUp() {
		service = new UserService(users, profiles, deletions, personalData);
		student = User.newEmailAccount(USER_ID, "asha@example.com", "hash", "Asha");
		profile = new UserProfileResponse(USER_ID, "asha@example.com", "Asha", Plan.FREE, Language.EN, 1.0,
				"light", false, false);
	}

	@Test
	void profileReturnsTheMappedAccount() {
		when(users.findById(USER_ID)).thenReturn(Optional.of(student));
		when(profiles.toProfile(student)).thenReturn(profile);

		assertThat(service.profile(USER_ID)).isEqualTo(profile);
	}

	@Test
	void anUnknownAccountIsNotFound() {
		when(users.findById(USER_ID)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> service.profile(USER_ID))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.NOT_FOUND));
	}

	@Test
	void updatePreferencesChangesTheAccountAndReturnsTheProfile() {
		when(users.findById(USER_ID)).thenReturn(Optional.of(student));
		when(profiles.toProfile(student)).thenReturn(profile);

		service.updatePreferences(USER_ID, new PreferencesRequest(Language.HI, 1.5, "dark"));

		assertThat(student.getLanguage()).isEqualTo(Language.HI);
		assertThat(student.getVoiceSpeed()).isEqualTo(1.5);
		assertThat(student.getTheme()).isEqualTo("dark");
	}

	@Test
	void exportPutsTheProfileFirstThenEveryModuleSection() {
		when(users.findById(USER_ID)).thenReturn(Optional.of(student));
		when(profiles.toProfile(student)).thenReturn(profile);
		when(personalData.export(USER_ID)).thenReturn(Map.of("lessons", "[]"));

		Map<String, Object> export = service.exportData(USER_ID);

		assertThat(export).containsEntry("profile", profile).containsEntry("lessons", "[]");
		assertThat(export.keySet()).containsExactly("profile", "lessons");
	}

	@Test
	void currentPlanReadsThePlanOfTheAccount() {
		when(users.findById(USER_ID)).thenReturn(Optional.of(student));

		assertThat(service.currentPlan(USER_ID)).isEqualTo(Plan.FREE);
	}

	@Test
	void requestDeletionDelegatesToTheDeletionService() {
		service.requestDeletion(USER_ID);

		verify(deletions).requestDeletion(USER_ID);
	}

	@Test
	void changePlanSetsThePlanAndItsEndDate() {
		Instant end = Instant.parse("2026-11-09T00:00:00Z");
		when(users.findById(USER_ID)).thenReturn(Optional.of(student));

		service.changePlan(USER_ID, Plan.PRO, end);

		assertThat(student.getPlan()).isEqualTo(Plan.PRO);
		assertThat(student.getPlanExpiresAt()).isEqualTo(end);
	}
}
