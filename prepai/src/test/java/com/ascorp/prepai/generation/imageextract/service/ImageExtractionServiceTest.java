package com.ascorp.prepai.generation.imageextract.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.generation.imageextract.model.Confidence;
import com.ascorp.prepai.generation.imageextract.model.ExtractedProblem;
import com.ascorp.prepai.generation.imageextract.model.ImageProperties;
import com.ascorp.prepai.generation.llm.model.LlmProviderException;
import com.ascorp.prepai.generation.llm.model.RetryReason;
import com.ascorp.prepai.generation.llm.service.LlmCaller;
import com.ascorp.prepai.generation.llm.service.LlmMetrics;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

class ImageExtractionServiceTest {

	private static final String READABLE = """
			Here is the reading: {"problemText": "A ball is thrown at 20 m/s. Find its range.",
			"confidence": "HIGH", "hasDiagram": true}""";
	private static final String LOW_CONFIDENCE =
			"{\"problemText\": \"A bal\", \"confidence\": \"LOW\", \"hasDiagram\": false}";
	private static final String EMPTY_TEXT =
			"{\"problemText\": \"\", \"confidence\": \"HIGH\", \"hasDiagram\": false}";
	private static final String NOT_JSON = "I cannot make out a problem in this photo.";
	private static final String PROVIDER_NAME = "scripted";

	private final SimpleMeterRegistry registry = new SimpleMeterRegistry();
	private final CircuitBreakerRegistry breakers = CircuitBreakerRegistry.ofDefaults();

	@Test
	void readsTheProblemFromAPhotoThroughTheModelOnce() {
		ScriptedReader reader = new ScriptedReader(READABLE);

		ExtractedProblem problem = service(reader).extract(ImageFixtures.pngWithMetadata());

		assertThat(problem.problemText()).isEqualTo("A ball is thrown at 20 m/s. Find its range.");
		assertThat(problem.confidence()).isEqualTo(Confidence.HIGH);
		assertThat(problem.hasDiagram()).isTrue();
		assertThat(reader.prompts()).hasSize(1);
		assertThat(reader.prompts().getFirst().image().mimeType()).isEqualTo("image/png");
		assertThat(registry.counter("prepai.llm.calls", "provider", PROVIDER_NAME, "purpose", "IMAGE_EXTRACT",
				"outcome", "SUCCESS").count()).isEqualTo(1);
	}

	@Test
	void refusesALowConfidenceReading() {
		assertUnreadable(new ScriptedReader(LOW_CONFIDENCE));
	}

	@Test
	void refusesAnEmptyProblemText() {
		assertUnreadable(new ScriptedReader(EMPTY_TEXT));
	}

	@Test
	void refusesAReplyThatIsNotTheJsonObject() {
		assertUnreadable(new ScriptedReader(NOT_JSON));
	}

	@Test
	void refusesAReplyWithNoText() {
		assertUnreadable(new ScriptedReader(null));
	}

	@Test
	void mapsAProviderFailureToLlmUnavailable() {
		ScriptedReader reader = new ScriptedReader(
				new LlmProviderException(RetryReason.TIMEOUT, new SocketTimeoutException()));

		assertCode(reader, ErrorCode.LLM_UNAVAILABLE);
	}

	@Test
	void refusesAnUnsupportedPhotoBeforeAnyModelCall() {
		ScriptedReader reader = new ScriptedReader(READABLE);

		byte[] gif = "GIF89a".getBytes(StandardCharsets.US_ASCII);

		assertThatThrownBy(() -> service(reader).extract(gif))
				.isInstanceOfSatisfying(ApiException.class,
						e -> assertThat(e.getCode()).isEqualTo(ErrorCode.IMAGE_UNSUPPORTED));
		assertThat(reader.prompts()).isEmpty();
	}

	private void assertUnreadable(ScriptedReader reader) {
		assertCode(reader, ErrorCode.IMAGE_UNREADABLE);
	}

	private void assertCode(ScriptedReader reader, ErrorCode code) {
		assertThatThrownBy(() -> service(reader).extract(ImageFixtures.pngWithMetadata()))
				.isInstanceOfSatisfying(ApiException.class, e -> assertThat(e.getCode()).isEqualTo(code));
	}

	private ImageExtractionService service(ScriptedReader reader) {
		LlmCaller caller = new LlmCaller(reader, breakers, new LlmMetrics(registry, breakers));
		ImageSanitizer sanitizer = new ImageSanitizer(new ImageProperties(1_000_000),
				List.of(new JpegMetadataStripper(), new PngMetadataStripper(), new WebpMetadataStripper()));
		return new ImageExtractionService(sanitizer, new ExtractionReplyParser(), caller);
	}
}
