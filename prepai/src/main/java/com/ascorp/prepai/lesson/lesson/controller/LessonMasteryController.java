package com.ascorp.prepai.lesson.lesson.controller;

import com.ascorp.prepai.lesson.lesson.model.dto.MasteryCheckRequest;
import com.ascorp.prepai.lesson.lesson.model.dto.MasteryCheckResponse;
import com.ascorp.prepai.lesson.lesson.service.MasteryCheckService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Marks the student's answer to a lesson's mastery check (spec 4.2). */
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonMasteryController {

	private final MasteryCheckService masteryChecks;

	@PostMapping("/{lessonId}/mastery-check")
	public MasteryCheckResponse masteryCheck(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID lessonId,
			@Valid @RequestBody MasteryCheckRequest answer) {
		return masteryChecks.answer(UUID.fromString(jwt.getSubject()), lessonId, answer.selectedOptionId());
	}
}
