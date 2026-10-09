package com.ascorp.prepai.common.errors;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

class ErrorResponseWriterTest {

	private final ErrorResponseWriter writer = new ErrorResponseWriter(JsonMapper.builder().build());

	@Test
	void writesTheCodeStatusAndJsonEnvelope() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();

		writer.write(response, ErrorCode.UNAUTHENTICATED);

		assertThat(response.getStatus()).isEqualTo(401);
		assertThat(response.getContentType()).startsWith("application/json");
		assertThat(response.getContentAsString()).contains("\"error\"", "\"code\":\"UNAUTHENTICATED\"");
	}

	@Test
	void leavesOutOptionalFieldsThatAreNotSet() throws IOException {
		MockHttpServletResponse response = new MockHttpServletResponse();

		writer.write(response, ErrorCode.FORBIDDEN);

		assertThat(response.getContentAsString()).doesNotContain("details", "retryAfterSeconds");
	}
}
