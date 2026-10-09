package com.ascorp.prepai.speech.tts.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

class WavFormatTest {

	private static final int MILLIS = 1500;
	private static final int HEADER_ONLY_BYTES = 44;
	private static final int DATA_CHUNK_START = 36;

	@Test
	void aWavFileReportsItsPlayingTime() {
		assertThat(WavFormat.durationMillis(WavFixtures.wav(MILLIS))).isEqualTo(MILLIS);
	}

	@Test
	void aFileCutAfterTheDataHeaderStillReportsItsTime() {
		byte[] head = Arrays.copyOf(WavFixtures.wav(MILLIS), HEADER_ONLY_BYTES);

		assertThat(WavFormat.durationMillis(head)).isEqualTo(MILLIS);
	}

	@Test
	void anOddSizedChunkBeforeTheDataIsSkippedWithItsPadding() {
		byte[] list = withOddChunkBeforeData(WavFixtures.wav(MILLIS));

		assertThat(WavFormat.durationMillis(list)).isEqualTo(MILLIS);
	}

	@Test
	void aFileWithoutADataChunkHasNoDuration() {
		assertThat(WavFormat.durationMillis(new byte[] {'R', 'I', 'F', 'F'})).isZero();
	}

	@Test
	void onlyRiffWaveBytesAreWav() {
		assertThat(WavFormat.isWav(WavFixtures.wav(MILLIS))).isTrue();
		assertThat(WavFormat.isWav("ID3 mp3 header bytes".getBytes(StandardCharsets.US_ASCII))).isFalse();
		assertThat(WavFormat.isWav(null)).isFalse();
	}

	private static byte[] withOddChunkBeforeData(byte[] wav) {
		byte[] odd = {'L', 'I', 'S', 'T', 3, 0, 0, 0, 'a', 'b', 'c', 0};
		byte[] result = new byte[wav.length + odd.length];
		System.arraycopy(wav, 0, result, 0, DATA_CHUNK_START);
		System.arraycopy(odd, 0, result, DATA_CHUNK_START, odd.length);
		System.arraycopy(wav, DATA_CHUNK_START, result, DATA_CHUNK_START + odd.length,
				wav.length - DATA_CHUNK_START);
		return result;
	}
}
