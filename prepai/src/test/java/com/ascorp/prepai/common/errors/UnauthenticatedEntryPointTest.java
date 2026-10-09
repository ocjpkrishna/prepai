package com.ascorp.prepai.common.errors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.BadCredentialsException;
import tools.jackson.databind.json.JsonMapper;

class UnauthenticatedEntryPointTest {

	@Test
	void answersAMissingTokenWith401InTheErrorShape() throws Exception {
		UnauthenticatedEntryPoint entryPoint =
				new UnauthenticatedEntryPoint(new ErrorResponseWriter(JsonMapper.builder().build()));
		MockHttpServletResponse response = new MockHttpServletResponse();

		entryPoint.commence(new MockHttpServletRequest(), response, new BadCredentialsException("no token"));

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentAsString()).contains("\"code\":\"UNAUTHENTICATED\"").doesNotContain("no token");
	}
}
