package com.ascorp.prepai.common.errors;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** Every error code of the API, with its HTTP status and a user-safe default message (spec 4.7). */
@Getter
public enum ErrorCode {

	VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "Some fields are invalid."),
	UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "Please log in again."),
	FORBIDDEN(HttpStatus.FORBIDDEN, "You don't have access to this."),
	EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "Please verify your email to continue."),
	CONSENT_REQUIRED(HttpStatus.FORBIDDEN, "Waiting for guardian consent."),
	NOT_FOUND(HttpStatus.NOT_FOUND, "Not found."),
	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "An account with this email already exists."),
	IMAGE_TOO_LARGE(HttpStatus.CONTENT_TOO_LARGE, "Image too large, try again."),
	IMAGE_UNSUPPORTED(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Use a JPEG, PNG or WebP photo."),
	IMAGE_UNREADABLE(HttpStatus.UNPROCESSABLE_CONTENT, "We couldn't read this photo. Please retake it."),
	PROBLEM_OUT_OF_SCOPE(HttpStatus.UNPROCESSABLE_CONTENT, "PrepAI covers Physics, Chemistry and Maths."),
	DAILY_LIMIT_REACHED(HttpStatus.TOO_MANY_REQUESTS, "You've used all of today's sessions."),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "Slow down a little."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong. Please try again."),
	LESSON_GENERATION_FAILED(HttpStatus.BAD_GATEWAY, "Couldn't build this lesson. No session was used."),
	LLM_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Tutor is busy, try again shortly."),
	TTS_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Audio is unavailable right now.");

	private final HttpStatus status;
	private final String defaultMessage;

	ErrorCode(HttpStatus status, String defaultMessage) {
		this.status = status;
		this.defaultMessage = defaultMessage;
	}
}
