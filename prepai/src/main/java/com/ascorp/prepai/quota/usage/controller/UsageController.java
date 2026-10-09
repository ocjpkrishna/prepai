package com.ascorp.prepai.quota.usage.controller;

import com.ascorp.prepai.quota.usage.model.dto.UsageResponse;
import com.ascorp.prepai.quota.usage.service.UsageService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** The student's plan and today's sessions (spec 4.3). The token subject is the user id. */
@RestController
@RequestMapping("/api/v1/users/me")
@RequiredArgsConstructor
public class UsageController {

	private final UsageService usage;

	@GetMapping("/usage")
	public UsageResponse summary(@AuthenticationPrincipal Jwt jwt) {
		return usage.summary(UUID.fromString(jwt.getSubject()));
	}
}
