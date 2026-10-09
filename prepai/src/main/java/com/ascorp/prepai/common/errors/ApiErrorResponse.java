package com.ascorp.prepai.common.errors;

import com.ascorp.prepai.common.errors.ApiError.FieldIssue;
import java.util.List;

/** The JSON envelope of every error: {"error": {...}}. */
public record ApiErrorResponse(ApiError error) {

	/** Builds the envelope with the trace ID of the current request, so support can find the logs. */
	public static ApiErrorResponse of(
			ErrorCode code, String message, List<FieldIssue> details, Long retryAfterSeconds) {
		return new ApiErrorResponse(
				new ApiError(code, message, details, retryAfterSeconds, TraceIdFilter.currentTraceId()));
	}
}
