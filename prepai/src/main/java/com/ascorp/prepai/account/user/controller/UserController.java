package com.ascorp.prepai.account.user.controller;

import com.ascorp.prepai.account.user.model.dto.PreferencesRequest;
import com.ascorp.prepai.account.user.model.dto.UserProfileResponse;
import com.ascorp.prepai.account.user.service.UserService;
import jakarta.validation.Valid;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The signed-in student's own account (spec 4.3). The token subject is the user id. */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UserController {

	private final UserService userService;

	@GetMapping
	public UserProfileResponse profile(@AuthenticationPrincipal Jwt jwt) {
		return userService.profile(userId(jwt));
	}

	@PutMapping("/preferences")
	public UserProfileResponse updatePreferences(@AuthenticationPrincipal Jwt jwt,
			@Valid @RequestBody PreferencesRequest request) {
		return userService.updatePreferences(userId(jwt), request);
	}

	@GetMapping("/export")
	public Map<String, Object> export(@AuthenticationPrincipal Jwt jwt) {
		return userService.exportData(userId(jwt));
	}

	@DeleteMapping
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void delete(@AuthenticationPrincipal Jwt jwt) {
		userService.requestDeletion(userId(jwt));
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
