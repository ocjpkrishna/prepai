package com.ascorp.prepai.account.auth.model.dto;

/** The tokens of a login or a refresh. expiresIn is the access token's lifetime in seconds. */
public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
