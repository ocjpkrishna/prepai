package com.ascorp.prepai.speech.tts.controller;

import com.ascorp.prepai.speech.tts.model.dto.SynthesizeRequest;
import com.ascorp.prepai.speech.tts.model.dto.SynthesizeResponse;
import com.ascorp.prepai.speech.tts.model.dto.VoiceDto;
import com.ascorp.prepai.speech.tts.service.TtsService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Narration audio (spec 4.5): synthesize a text, list the voices. Both need a logged-in student. */
@RestController
@RequestMapping("/api/v1/tts")
@RequiredArgsConstructor
public class TtsController {

	private final TtsService tts;

	@PostMapping("/synthesize")
	public SynthesizeResponse synthesize(@Valid @RequestBody SynthesizeRequest request) {
		return tts.synthesize(request.text(), request.language());
	}

	@GetMapping("/voices")
	public List<VoiceDto> voices() {
		return tts.voices();
	}
}
