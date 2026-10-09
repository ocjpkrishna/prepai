package com.ascorp.prepai.account.privacy.controller;

import com.ascorp.prepai.account.privacy.service.GuardianConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** The public guardian consent link (spec 4.1, 2.7). */
@RestController
@RequestMapping("/api/v1/auth/guardian-consent")
@RequiredArgsConstructor
public class GuardianConsentController {

	private final GuardianConsentService guardianConsent;

	@PostMapping("/confirm")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void confirm(@RequestParam(defaultValue = "") String token) {
		guardianConsent.confirm(token);
	}
}
