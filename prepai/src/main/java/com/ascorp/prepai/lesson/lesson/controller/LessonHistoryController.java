package com.ascorp.prepai.lesson.lesson.controller;

import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.lesson.lesson.model.dto.LessonHistoryPage;
import com.ascorp.prepai.lesson.lesson.service.LessonHistoryService;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** The student's past lessons (spec 4.2). */
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonHistoryController {

	private final LessonHistoryService history;

	@GetMapping("/history")
	public LessonHistoryPage history(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Subject subject,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return history.history(userId(jwt), subject, page, size);
	}

	@GetMapping("/{lessonId}")
	public LessonResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID lessonId) {
		return history.get(userId(jwt), lessonId);
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
