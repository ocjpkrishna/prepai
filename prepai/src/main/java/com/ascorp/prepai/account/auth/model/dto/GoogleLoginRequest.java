package com.ascorp.prepai.account.auth.model.dto;

import jakarta.validation.constraints.NotBlank;

/** The Google ID token the frontend got from Google's sign-in button. */
public record GoogleLoginRequest(@NotBlank String idToken) {
}
