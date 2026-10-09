package com.ascorp.prepai.lesson.lesson.controller;

import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.lesson.lesson.model.dto.ExtractResponse;
import com.ascorp.prepai.lesson.lesson.model.dto.FeedbackRequest;
import com.ascorp.prepai.lesson.lesson.model.dto.LessonHistoryPage;
import com.ascorp.prepai.lesson.lesson.model.dto.MasteryCheckRequest;
import com.ascorp.prepai.lesson.lesson.model.dto.MasteryCheckResponse;
import com.ascorp.prepai.lesson.lesson.service.LessonExtractService;
import com.ascorp.prepai.lesson.lesson.service.LessonHistoryService;
import com.ascorp.prepai.lesson.lesson.service.LessonService;
import com.ascorp.prepai.lesson.lesson.service.MasteryCheckService;
import jakarta.validation.Valid;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** The lesson API (spec 4.2). The token subject is the user id. */
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonController {

	private final LessonService lessons;
	private final LessonExtractService extraction;
	private final LessonHistoryService history;
	private final MasteryCheckService masteryChecks;

	@PostMapping("/generate")
	public LessonResponse generate(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody LessonRequest request) {
		return lessons.generateLesson(userId(jwt), request);
	}

	@PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ExtractResponse extract(@AuthenticationPrincipal Jwt jwt, @RequestParam("image") MultipartFile image)
			throws IOException {
		return extraction.extract(userId(jwt), image.getBytes());
	}

	@GetMapping("/history")
	public LessonHistoryPage history(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Subject subject,
			@RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
		return history.history(userId(jwt), subject, page, size);
	}

	@GetMapping("/{lessonId}")
	public LessonResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID lessonId) {
		return history.get(userId(jwt), lessonId);
	}

	@PostMapping("/{lessonId}/feedback")
	@ResponseStatus(HttpStatus.NO_CONTENT)
	public void feedback(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID lessonId,
			@Valid @RequestBody FeedbackRequest feedback) {
		history.rate(userId(jwt), lessonId, feedback);
	}

	@PostMapping("/{lessonId}/mastery-check")
	public MasteryCheckResponse masteryCheck(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID lessonId,
			@Valid @RequestBody MasteryCheckRequest answer) {
		return masteryChecks.answer(userId(jwt), lessonId, answer.selectedOptionId());
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
