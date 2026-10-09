package com.ascorp.prepai.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * The student journey over HTTP with the fake lesson provider: register, log in, refresh, generate, history,
 * lesson, feedback and the free daily limit. Needs the PostgreSQL and Redis of the VPS, so it is tagged "db".
 */
@Tag("db")
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class LessonFlowIntegrationTest {

	private static final String AUTH = "/api/v1/auth";
	private static final String LESSONS = "/api/v1/lessons";
	private static final String JSON = "application/json";
	private static final String AUTHORIZATION = "Authorization";
	private static final String BEARER = "Bearer ";
	private static final String PASSWORD = "long-enough-1";
	private static final int FREE_SESSIONS_PER_DAY = 3;

	@Autowired
	private MockMvc mvc;

	@Test
	void aStudentRegistersLogsInAndRefreshesTheirSession() throws Exception {
		String email = newEmail();
		String refreshToken = JsonPath.read(register(email), "$.refreshToken");
		assertThat(login(email)).contains("accessToken");

		mvc.perform(post(AUTH + "/refresh").contentType(JSON).content(refreshBody(refreshToken)))
				.andExpect(status().isOk());
		mvc.perform(post(AUTH + "/refresh").contentType(JSON).content(refreshBody(refreshToken)))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
	}

	@Test
	void aStudentGeneratesReadsReviewsAndHitsTheFreeDailyLimit() throws Exception {
		String access = JsonPath.read(register(newEmail()), "$.accessToken");

		String lessonId = generate(access, "A ball is thrown at 20 m/s; find its range.");
		mvc.perform(get(LESSONS + "/history").header(AUTHORIZATION, BEARER + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.items[0].lessonId").value(lessonId));
		mvc.perform(get(LESSONS + "/" + lessonId).header(AUTHORIZATION, BEARER + access))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.lessonId").value(lessonId));
		mvc.perform(post(LESSONS + "/" + lessonId + "/feedback").header(AUTHORIZATION, BEARER + access)
				.contentType(JSON).content("{\"rating\":4,\"comment\":\"clear\"}"))
				.andExpect(status().isNoContent());

		for (int session = 1; session < FREE_SESSIONS_PER_DAY; session++) {
			generate(access, "Practice question number " + session + " on lenses.");
		}
		mvc.perform(post(LESSONS + "/generate").header(AUTHORIZATION, BEARER + access)
				.contentType(JSON).content(problemBody("One question too many today.")))
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error.code").value("DAILY_LIMIT_REACHED"));
	}

	private String register(String email) throws Exception {
		String body = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\",\"name\":\"Asha\","
				+ "\"birthDate\":\"2000-01-01\",\"acceptsTerms\":true}";
		return mvc.perform(post(AUTH + "/register").contentType(JSON).content(body))
				.andExpect(status().isCreated())
				.andReturn().getResponse().getContentAsString();
	}

	private String login(String email) throws Exception {
		String body = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
		return mvc.perform(post(AUTH + "/login").contentType(JSON).content(body))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
	}

	private String generate(String access, String text) throws Exception {
		MvcResult result = mvc.perform(post(LESSONS + "/generate").header(AUTHORIZATION, BEARER + access)
				.contentType(JSON).content(problemBody(text)))
				.andExpect(status().isOk())
				.andReturn();
		return JsonPath.read(result.getResponse().getContentAsString(), "$.lessonId");
	}

	private static String problemBody(String text) {
		return "{\"type\":\"PROBLEM\",\"subject\":\"PHYSICS\",\"exam\":\"JEE_MAIN\",\"input\":{\"text\":\"" + text
				+ "\"},\"difficulty\":\"MEDIUM\",\"language\":\"en\"}";
	}

	private static String refreshBody(String refreshToken) {
		return "{\"refreshToken\":\"" + refreshToken + "\"}";
	}

	private static String newEmail() {
		return "student-" + UUID.randomUUID() + "@example.com";
	}
}
