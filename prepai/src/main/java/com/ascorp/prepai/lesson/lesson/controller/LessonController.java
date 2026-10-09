package com.ascorp.prepai.lesson.lesson.controller;

import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.lesson.lesson.service.LessonService;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Generates a lesson (spec 4.2). The token subject is the user id. */
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonController {

	private final LessonService lessons;

	@PostMapping("/generate")
	public LessonResponse generate(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LessonRequest request) {
		return lessons.generateLesson(UUID.fromString(jwt.getSubject()), request);
	}
}
