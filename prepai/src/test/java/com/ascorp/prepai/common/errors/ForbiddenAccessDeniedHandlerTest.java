package com.ascorp.prepai.common.errors;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import tools.jackson.databind.json.JsonMapper;

class ForbiddenAccessDeniedHandlerTest {

	@Test
	void answersADeniedRequestWith403InTheErrorShape() throws Exception {
		ForbiddenAccessDeniedHandler handler =
				new ForbiddenAccessDeniedHandler(new ErrorResponseWriter(JsonMapper.builder().build()));
		MockHttpServletResponse response = new MockHttpServletResponse();

		handler.handle(new MockHttpServletRequest(), response, new AccessDeniedException("not owner"));

		assertThat(response.getStatus()).isEqualTo(403);
		assertThat(response.getContentAsString()).contains("\"code\":\"FORBIDDEN\"").doesNotContain("not owner");
	}
}
