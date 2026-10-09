package com.ascorp.prepai.speech.tts.model.dto;

import com.ascorp.prepai.speech.tts.model.TtsLimits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Body of POST /api/v1/tts/synthesize (spec 4.5). The speed is not a parameter: audio is always normal speed. */
public record SynthesizeRequest(
		@NotBlank @Size(max = TtsLimits.MAX_TEXT_LENGTH) String text,
		@NotBlank @Pattern(regexp = "en-IN|hi-IN") String language) {
}
