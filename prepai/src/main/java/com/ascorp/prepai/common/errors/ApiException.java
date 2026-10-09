package com.ascorp.prepai.common.errors;

import lombok.Getter;

/**
 * The only exception modules throw for an expected failure. {@link GlobalExceptionHandler} turns it into the
 * spec 4.7 error format, so no module writes its own handler.
 */
@Getter
public class ApiException extends RuntimeException {

	private final ErrorCode code;
	private final Long retryAfterSeconds;

	public ApiException(ErrorCode code) {
		this(code, code.getDefaultMessage());
	}

	public ApiException(ErrorCode code, String message) {
		this(code, message, null);
	}

	public ApiException(ErrorCode code, String message, Long retryAfterSeconds) {
		super(message);
		this.code = code;
		this.retryAfterSeconds = retryAfterSeconds;
	}
}
