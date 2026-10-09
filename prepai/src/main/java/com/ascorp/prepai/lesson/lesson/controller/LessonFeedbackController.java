package com.ascorp.prepai.lesson.lesson.controller;

import com.ascorp.prepai.lesson.lesson.model.dto.FeedbackRequest;
import com.ascorp.prepai.lesson.lesson.service.LessonHistoryService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** A student's rating of a lesson (spec 4.2). */
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonFeedbackController {

	private final LessonHistoryService history;

	@PostMapping("/{lessonId}/feedback")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void feedback(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID lessonId,
			@Valid @RequestBody FeedbackRequest feedback) {
		history.rate(UUID.fromString(jwt.getSubject()), lessonId, feedback);
	}
}
