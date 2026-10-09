package com.ascorp.prepai.speech.tts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import com.ascorp.prepai.speech.tts.model.VoiceStudioRequest;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

class AudioKeyTest {

	private static final String VOICE = "en-IN-default";

	@Test
	void identicalNarrationGetsTheSameName() {
		assertThat(AudioKey.of(new VoiceStudioRequest("Hello", "en-IN", VOICE)))
				.isEqualTo(AudioKey.of(new VoiceStudioRequest("Hello", "en-IN", VOICE)));
	}

	@Test
	void theLanguageAndTheVoiceChangeTheName() {
		String base = AudioKey.of(new VoiceStudioRequest("Hello", "en-IN", VOICE));

		assertThat(AudioKey.of(new VoiceStudioRequest("Hello", "hi-IN", VOICE))).isNotEqualTo(base);
		assertThat(AudioKey.of(new VoiceStudioRequest("Hello", "en-IN", "other"))).isNotEqualTo(base);
	}

	@Test
	void aTextThatMovesIntoTheLanguageFieldGetsADifferentName() {
		String split = AudioKey.of(new VoiceStudioRequest("a", "bc", VOICE));

		assertThat(AudioKey.of(new VoiceStudioRequest("ab", "c", VOICE))).isNotEqualTo(split);
	}

	@Test
	void theNameIsASha256HexDigest() {
		assertThat(AudioKey.of(new VoiceStudioRequest("Hello", "en-IN", VOICE))).hasSize(64).matches("[0-9a-f]+");
	}

	@Test
	void aRuntimeWithoutSha256CannotNameAudio() {
		try (MockedStatic<MessageDigest> digests = mockStatic(MessageDigest.class)) {
			digests.when(() -> MessageDigest.getInstance(anyString()))
					.thenThrow(new NoSuchAlgorithmException("no SHA-256 here"));

			assertThatThrownBy(() -> AudioKey.of(new VoiceStudioRequest("Hello", "en-IN", VOICE)))
					.isInstanceOf(IllegalStateException.class);
		}
	}
}
