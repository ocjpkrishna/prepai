package com.ascorp.prepai.account.user.service;

import com.ascorp.prepai.account.auth.model.entity.User;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.privacy.service.AccountDeletionService;
import com.ascorp.prepai.account.privacy.service.PersonalDataService;
import com.ascorp.prepai.account.user.mapper.UserProfileMapper;
import com.ascorp.prepai.account.user.model.dto.PreferencesRequest;
import com.ascorp.prepai.account.user.model.dto.UserProfileResponse;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.enums.Plan;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** The student's own profile, preferences, data download and deletion (spec 4.3). Other modules read the plan here. */
@Service
@RequiredArgsConstructor
public class UserService {

	private final UserRepository users;
	private final UserProfileMapper profiles;
	private final AccountDeletionService deletions;
	private final PersonalDataService personalData;

	@Transactional(readOnly = true)
	public UserProfileResponse profile(UUID userId) {
		return profiles.toProfile(findUser(userId));
	}

	@Transactional
	public UserProfileResponse updatePreferences(UUID userId, PreferencesRequest request) {
		User student = findUser(userId);
		student.updatePreferences(request.language(), request.voiceSpeed(), request.theme());
		return profiles.toProfile(student);
	}

	/** The profile plus every module's section (lessons, feedback, usage as they are built). */
	@Transactional(readOnly = true)
	public Map<String, Object> exportData(UUID userId) {
		Map<String, Object> data = new LinkedHashMap<>();
		data.put("profile", profile(userId));
		data.putAll(personalData.export(userId));
		return data;
	}

	@Transactional(readOnly = true)
	public Plan currentPlan(UUID userId) {
		return findUser(userId).getPlan();
	}

	/** Called by billing when a payment starts, renews or ends a plan. */
	@Transactional
	public void changePlan(UUID userId, Plan plan, Instant expiresAt) {
		findUser(userId).changePlan(plan, expiresAt);
	}

	public void requestDeletion(UUID userId) {
		deletions.requestDeletion(userId);
	}

	private User findUser(UUID userId) {
		return users.findById(userId)
				.orElseThrow(() -> new ApiException(ErrorCode.NOT_FOUND, "We couldn't find this account."));
	}
}
