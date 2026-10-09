package com.ascorp.prepai.lesson.lesson.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** A star rating and an optional comment (spec 4.2). */
public record FeedbackRequest(
		@Min(FeedbackRequest.MIN_RATING) @Max(FeedbackRequest.MAX_RATING) int rating,
		@Size(max = FeedbackRequest.MAX_COMMENT_LENGTH) String comment) {

	public static final int MIN_RATING = 1;
	public static final int MAX_RATING = 5;
	public static final int MAX_COMMENT_LENGTH = 2000;
}
