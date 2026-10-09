package com.ascorp.prepai.speech.tts.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.speech.tts.config.VoiceStudioProperties;
import com.ascorp.prepai.speech.tts.model.VoiceStudioRequest;
import com.ascorp.prepai.speech.tts.model.dto.VoiceDto;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Calls VoiceStudio over HTTP at prepai.voicestudio.api-url. Every failure (refused, timed out, 5xx, not WAV)
 * becomes TTS_UNAVAILABLE, so a lesson never fails because of audio.
 */
@Slf4j
@Component
public class HttpVoiceStudioClient implements VoiceStudioClient {

	private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(3);
	private static final Duration READ_TIMEOUT = Duration.ofSeconds(30);
	private static final String SYNTHESIZE_PATH = "/synthesize";
	private static final String VOICES_PATH = "/voices";

	private final RestClient http;

	public HttpVoiceStudioClient(VoiceStudioProperties properties) {
		this.http = RestClient.builder()
				.baseUrl(properties.apiUrl())
				.requestFactory(requestFactory())
				.build();
	}

	@Override
	public byte[] synthesize(VoiceStudioRequest request) {
		byte[] wav = call(() -> http.post()
				.uri(SYNTHESIZE_PATH)
				.contentType(MediaType.APPLICATION_JSON)
				.body(request)
				.retrieve()
				.body(byte[].class));
		if (!WavFormat.isWav(wav)) {
			throw unavailable("the server did not return a WAV file");
		}
		return wav;
	}

	@Override
	public List<VoiceDto> voices() {
		VoiceDto[] voices = call(() -> http.get()
				.uri(VOICES_PATH)
				.retrieve()
				.body(VoiceDto[].class));
		if (voices == null) {
			throw unavailable("the server sent no voice list");
		}
		return List.of(voices);
	}

	private static JdkClientHttpRequestFactory requestFactory() {
		HttpClient client = HttpClient.newBuilder().connectTimeout(CONNECT_TIMEOUT).build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(client);
		factory.setReadTimeout(READ_TIMEOUT);
		return factory;
	}

	private static <T> T call(Supplier<T> request) {
		try {
			return request.get();
		} catch (RestClientException e) {
			throw unavailable(e.getMessage());
		}
	}

	private static ApiException unavailable(String reason) {
		log.warn("VoiceStudio unavailable: {}", reason);
		return new ApiException(ErrorCode.TTS_UNAVAILABLE);
	}
}
