package com.ascorp.prepai.lesson.lesson.service;

import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.generation.imageextract.model.Confidence;
import com.ascorp.prepai.generation.imageextract.model.ExtractedProblem;
import com.ascorp.prepai.generation.imageextract.service.ImageExtractionService;
import com.ascorp.prepai.quota.ratelimit.service.ExtractLimiter;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonInputResolverTest {

	private static final byte[] PHOTO = {1, 2, 3};

	@Mock
	private ExtractLimiter extractLimiter;

	@Mock
	private ImageExtractionService extraction;

	@InjectMocks
	private LessonInputResolver resolver;

	@Test
	void aProblemWithTextPassesThroughUntouched() {
		LessonRequest request = LessonFixtures.problem();

		assertThat(resolver.resolve(USER_ID, request)).isSameAs(request);
		verifyNoInteractions(extraction);
	}

	@Test
	void aProblemWithoutTextIsRejected() {
		LessonRequest request = LessonFixtures.request(LessonRequest.Type.TOPIC, "  ", null);

		assertThatThrownBy(() -> resolver.resolve(USER_ID, request)).isInstanceOf(ApiException.class);
	}

	@Test
	void aPhotoIsReadAndContinuesAsAProblem() {
		String encoded = Base64.getEncoder().encodeToString(PHOTO);
		when(extraction.extract(PHOTO)).thenReturn(new ExtractedProblem("Find x.", Confidence.HIGH, false));

		LessonRequest image = LessonFixtures.request(LessonRequest.Type.IMAGE, null, encoded);
		LessonRequest resolved = resolver.resolve(USER_ID, image);

		assertThat(resolved.type()).isEqualTo(LessonRequest.Type.PROBLEM);
		assertThat(resolved.input().text()).isEqualTo("Find x.");
		assertThat(resolved.input().imageBase64()).isNull();
	}

	@Test
	void aMissingPhotoIsRejected() {
		LessonRequest request = LessonFixtures.request(LessonRequest.Type.IMAGE, null, null);

		assertThatThrownBy(() -> resolver.resolve(USER_ID, request)).isInstanceOf(ApiException.class)
				.extracting(failure -> ((ApiException) failure).getCode()).isEqualTo(ErrorCode.VALIDATION_FAILED);
	}

	@Test
	void textThatIsNotBase64IsAnUnsupportedImage() {
		LessonRequest request = LessonFixtures.request(LessonRequest.Type.IMAGE, null, "***not base64***");

		assertThatThrownBy(() -> resolver.resolve(USER_ID, request)).isInstanceOf(ApiException.class)
				.extracting(failure -> ((ApiException) failure).getCode()).isEqualTo(ErrorCode.IMAGE_UNSUPPORTED);
	}
}
