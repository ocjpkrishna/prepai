package com.ascorp.prepai.lesson.lesson.controller;

import com.ascorp.prepai.lesson.lesson.model.dto.ExtractResponse;
import com.ascorp.prepai.lesson.lesson.service.LessonExtractService;
import java.io.IOException;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Reads a photographed problem into text (spec 4.2). */
@RestController
@RequestMapping("/api/v1/lessons")
@RequiredArgsConstructor
public class LessonExtractController {

	private final LessonExtractService extraction;

	@PostMapping(value = "/extract", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ExtractResponse extract(@AuthenticationPrincipal Jwt jwt, @RequestParam("image") MultipartFile image)
			throws IOException {
		return extraction.extract(userId(jwt), image.getBytes());
	}

	private static UUID userId(Jwt jwt) {
		return UUID.fromString(jwt.getSubject());
	}
}
