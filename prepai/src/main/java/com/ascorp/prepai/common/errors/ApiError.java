package com.ascorp.prepai.common.errors;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** The error object every failed response carries (spec 4.7). Optional fields are left out of the JSON. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ApiError(
		ErrorCode code,
		String message,
		List<FieldIssue> details,
		Long retryAfterSeconds,
		String traceId) {

	/** Which request field failed and why. */
	public record FieldIssue(String field, String issue) {
	}
}
