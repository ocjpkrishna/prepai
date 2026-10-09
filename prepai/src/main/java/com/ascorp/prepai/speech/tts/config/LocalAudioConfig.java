package com.ascorp.prepai.speech.tts.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Local development only: serves the audio folder at /audio/, the job nginx does in production (spec 4.5).
 * File names are content hashes, so they cannot be guessed.
 */
@Configuration
@Profile("local")
@RequiredArgsConstructor
public class LocalAudioConfig implements WebMvcConfigurer {

	private final AudioProperties audio;

	@Override
	public void addResourceHandlers(ResourceHandlerRegistry registry) {
		registry.addResourceHandler("/audio/**")
				.addResourceLocations(audio.dir().toAbsolutePath().toUri().toString());
	}
}
