package com.ascorp.prepai.speech.tts.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.support.GenericWebApplicationContext;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;

class LocalAudioConfigTest {

	@TempDir
	Path audioDir;

	@Test
	void theAudioFolderIsServedUnderAudio() {
		AudioProperties audio = new AudioProperties(audioDir, 7, 1, 2);
		ResourceHandlerRegistry registry = new ResourceHandlerRegistry(new GenericWebApplicationContext(),
				new MockServletContext());

		new LocalAudioConfig(audio).addResourceHandlers(registry);

		assertThat(registry.hasMappingForPattern("/audio/**")).isTrue();
		assertThat(registry.hasMappingForPattern("/other/**")).isFalse();
	}
}
