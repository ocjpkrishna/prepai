package com.ascorp.prepai.speech.tts.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.speech.tts.config.VoiceStudioProperties;
import com.ascorp.prepai.speech.tts.model.VoiceStudioRequest;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Runs against a tiny local HTTP server that plays VoiceStudio, so the real HTTP code and its errors are tested. */
class HttpVoiceStudioClientTest {

	private static final VoiceStudioRequest REQUEST = new VoiceStudioRequest("Hello", "en-IN", "en-IN-default");
	private static final String WAV_TYPE = "audio/wav";
	private static final String JSON_TYPE = "application/json";
	private static final String TEXT_TYPE = "text/plain";
	private static final String VOICES_JSON =
			"[{\"id\":\"en-IN-default\",\"name\":\"English (India)\",\"language\":\"en-IN\"}]";

	private HttpServer server;

	@AfterEach
	void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void aWavAnswerIsReturnedAsItIs() {
		byte[] wav = WavFixtures.wav(WavFixtures.DEFAULT_MILLIS);

		assertThat(clientFor(serve(200, WAV_TYPE, wav)).synthesize(REQUEST)).isEqualTo(wav);
	}

	@Test
	void aServerErrorIsTtsUnavailable() {
		HttpVoiceStudioClient client = clientFor(serve(500, TEXT_TYPE, new byte[0]));

		assertUnavailable(() -> client.synthesize(REQUEST));
	}

	@Test
	void anAnswerThatIsNotWavIsTtsUnavailable() {
		HttpVoiceStudioClient client = clientFor(serve(200, TEXT_TYPE, "not audio".getBytes(StandardCharsets.UTF_8)));

		assertUnavailable(() -> client.synthesize(REQUEST));
	}

	@Test
	void aServerThatIsNotRunningIsTtsUnavailable() {
		HttpVoiceStudioClient client = clientFor(serve(200, WAV_TYPE, WavFixtures.wav(WavFixtures.DEFAULT_MILLIS)));
		server.stop(0);

		assertUnavailable(() -> client.synthesize(REQUEST));
	}

	@Test
	void theVoiceListIsReadFromTheServer() {
		HttpVoiceStudioClient client = clientFor(serve(200, JSON_TYPE, VOICES_JSON.getBytes(StandardCharsets.UTF_8)));

		assertThat(client.voices()).isEqualTo(FakeVoiceStudioClient.VOICES);
	}

	private HttpVoiceStudioClient clientFor(String url) {
		return new HttpVoiceStudioClient(new VoiceStudioProperties(url, "en-IN-default"));
	}

	/** Starts a server that answers every request with the same status, type and body; returns its base URL. */
	private String serve(int status, String contentType, byte[] body) {
		try {
			server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
			server.createContext("/", exchange -> {
				exchange.getRequestBody().readAllBytes();
				exchange.getResponseHeaders().set("Content-Type", contentType);
				exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
				exchange.getResponseBody().write(body);
				exchange.close();
			});
			server.start();
			return "http://127.0.0.1:" + server.getAddress().getPort();
		} catch (IOException e) {
			throw new IllegalStateException("The local test server did not start", e);
		}
	}

	private static void assertUnavailable(ThrowingCallable call) {
		assertThatThrownBy(call).isInstanceOfSatisfying(ApiException.class,
				e -> assertThat(e.getCode()).isEqualTo(ErrorCode.TTS_UNAVAILABLE));
	}
}
