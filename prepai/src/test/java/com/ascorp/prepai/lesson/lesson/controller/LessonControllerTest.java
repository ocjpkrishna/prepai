package com.ascorp.prepai.lesson.lesson.controller;

import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.LESSON_ID;
import static com.ascorp.prepai.lesson.lesson.service.LessonFixtures.USER_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ascorp.prepai.common.errors.GlobalExceptionHandler;
import com.ascorp.prepai.generation.imageextract.model.Confidence;
import com.ascorp.prepai.lesson.lesson.model.dto.ExtractResponse;
import com.ascorp.prepai.lesson.lesson.model.dto.FeedbackRequest;
import com.ascorp.prepai.lesson.lesson.model.dto.LessonHistoryPage;
import com.ascorp.prepai.lesson.lesson.model.dto.MasteryCheckResponse;
import com.ascorp.prepai.lesson.lesson.service.LessonExtractService;
import com.ascorp.prepai.lesson.lesson.service.LessonFixtures;
import com.ascorp.prepai.lesson.lesson.service.LessonHistoryService;
import com.ascorp.prepai.lesson.lesson.service.LessonService;
import com.ascorp.prepai.lesson.lesson.service.MasteryCheckService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.web.method.annotation.AuthenticationPrincipalArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LessonControllerTest {

	private static final String BODY = """
			{"type":"PROBLEM","subject":"PHYSICS","exam":"JEE_MAIN","input":{"text":"A ball"},
			 "difficulty":"MEDIUM","language":"en"}""";

	@Mock
	private LessonService lessons;

	@Mock
	private LessonExtractService extraction;

	@Mock
	private LessonHistoryService history;

	@Mock
	private MasteryCheckService masteryChecks;

	private MockMvc mvc;
	private JwtAuthenticationToken student;

	@BeforeEach
	void setUp() {
		Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(USER_ID.toString()).build();
		student = new JwtAuthenticationToken(jwt, AuthorityUtils.NO_AUTHORITIES);
		mvc = MockMvcBuilders.standaloneSetup(new LessonController(lessons), new LessonExtractController(extraction),
				new LessonHistoryController(history), new LessonFeedbackController(history),
				new MasteryCheckController(masteryChecks))
				.setControllerAdvice(new GlobalExceptionHandler())
				.setCustomArgumentResolvers(
						new AuthenticationPrincipalArgumentResolver())
				.build();
	}

	@Test
	void generateReturnsTheLesson() throws Exception {
		when(lessons.generateLesson(eq(USER_ID), any())).thenReturn(LessonFixtures.lesson());

		mvc.perform(post("/api/v1/lessons/generate").principal(student).contentType("application/json").content(BODY))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.title").value("Projectile Motion"));
	}

	@Test
	void generateRejectsABodyWithoutASubject() throws Exception {
		mvc.perform(post("/api/v1/lessons/generate").principal(student).contentType("application/json")
				.content("{\"type\":\"PROBLEM\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void extractReadsTheImagePart() throws Exception {
		when(extraction.extract(eq(USER_ID), any())).thenReturn(new ExtractResponse("Find x.", Confidence.HIGH, false));

		mvc.perform(multipart("/api/v1/lessons/extract").file(new MockMultipartFile("image", "p.png", "image/png",
				new byte[] {1})).principal(student))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.problemText").value("Find x."))
				.andExpect(jsonPath("$.confidence").value("HIGH"));
	}

	@Test
	void extractWithoutTheImagePartIsAValidationFailure() throws Exception {
		mvc.perform(multipart("/api/v1/lessons/extract").file(new MockMultipartFile("other", new byte[] {1}))
				.principal(student))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_FAILED"));
	}

	@Test
	void historyPassesTheFiltersOn() throws Exception {
		when(history.history(eq(USER_ID), any(), eq(1), eq(10)))
				.thenReturn(new LessonHistoryPage(List.of(), 1, 10, 0, 0));

		mvc.perform(get("/api/v1/lessons/history?page=1&size=10&subject=PHYSICS").principal(student))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.size").value(10));
	}

	@Test
	void anUnknownSubjectFilterIsAValidationFailure() throws Exception {
		mvc.perform(get("/api/v1/lessons/history?subject=ART").principal(student))
				.andExpect(status().isBadRequest());
	}

	@Test
	void getReturnsTheStoredLesson() throws Exception {
		when(history.get(USER_ID, LESSON_ID)).thenReturn(LessonFixtures.lesson());

		mvc.perform(get("/api/v1/lessons/" + LESSON_ID).principal(student))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.subject").value("PHYSICS"));
	}

	@Test
	void feedbackAnswersNoContentAndRejectsAnOutOfRangeRating() throws Exception {
		mvc.perform(post("/api/v1/lessons/" + LESSON_ID + "/feedback").principal(student)
				.contentType("application/json").content("{\"rating\":5,\"comment\":\"Great\"}"))
				.andExpect(status().isNoContent());
		verify(history).rate(USER_ID, LESSON_ID, new FeedbackRequest(5, "Great"));

		mvc.perform(post("/api/v1/lessons/" + LESSON_ID + "/feedback").principal(student)
				.contentType("application/json").content("{\"rating\":6}"))
				.andExpect(status().isBadRequest());
	}

	@Test
	void masteryCheckAnswersCorrectness() throws Exception {
		when(masteryChecks.answer(USER_ID, LESSON_ID, "B")).thenReturn(new MasteryCheckResponse(true, "Because."));

		mvc.perform(post("/api/v1/lessons/" + LESSON_ID + "/mastery-check").principal(student)
				.contentType("application/json").content("{\"selectedOptionId\":\"B\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.correct").value(true));
	}
}
