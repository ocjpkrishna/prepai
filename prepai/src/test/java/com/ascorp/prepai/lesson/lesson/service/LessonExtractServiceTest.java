package com.ascorp.prepai.lesson.lesson.service;

import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.USER_ID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.privacy.service.AccountGateService;
import com.ascorp.prepai.generation.imageextract.model.Confidence;
import com.ascorp.prepai.generation.imageextract.model.ExtractedProblem;
import com.ascorp.prepai.generation.imageextract.service.ImageExtractionService;
import com.ascorp.prepai.lesson.lesson.model.dto.ExtractResponse;
import com.ascorp.prepai.quota.ratelimit.service.ExtractLimiter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LessonExtractServiceTest {

	private static final byte[] PHOTO = {9, 9};

	@Mock
	private AccountGateService accountGate;

	@Mock
	private ExtractLimiter extractLimiter;

	@Mock
	private ImageExtractionService extraction;

	@InjectMocks
	private LessonExtractService service;

	@Test
	void theTextReadFromAPhotoIsReturnedAfterTheGateAndTheLimit() {
		when(extraction.extract(PHOTO)).thenReturn(new ExtractedProblem("Find x.", Confidence.MEDIUM, true));

		ExtractResponse response = service.extract(USER_ID, PHOTO);

		assertThat(response).isEqualTo(new ExtractResponse("Find x.", Confidence.MEDIUM, true));
		InOrder order = inOrder(accountGate, extractLimiter, extraction);
		order.verify(accountGate).requireLessonAccess(USER_ID);
		order.verify(extractLimiter).assertWithinExtractLimit(USER_ID);
		order.verify(extraction).extract(PHOTO);
	}
}
