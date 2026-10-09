package com.ascorp.prepai.account.auth.controller;

import com.ascorp.prepai.account.auth.model.dto.GoogleLoginRequest;
import com.ascorp.prepai.account.auth.model.dto.LoginRequest;
import com.ascorp.prepai.account.auth.model.dto.RefreshRequest;
import com.ascorp.prepai.account.auth.model.dto.RegisterRequest;
import com.ascorp.prepai.account.auth.model.dto.TokenResponse;
import com.ascorp.prepai.account.auth.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	@PostMapping("/register")
	@ResponseStatus(HttpStatus.CREATED)
	public TokenResponse register(@Valid @RequestBody RegisterRequest request) {
		return authService.register(request);
	}

	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@PostMapping("/refresh")
	public TokenResponse refresh(@Valid @RequestBody RefreshRequest request) {
		return authService.refresh(request);
	}

	@PostMapping("/google")
	public TokenResponse google(@Valid @RequestBody GoogleLoginRequest request) {
		return authService.loginWithGoogle(request);
	}

	@GetMapping("/verify-email")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void verifyEmail(@RequestParam(defaultValue = "") String token) {
		authService.verifyEmail(token);
	}
}
