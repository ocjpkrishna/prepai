package com.ascorp.prepai.common.errors;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class TraceIdFilterTest {

	private static final UUID FIXED_ID = UUID.fromString("7f3c9a1e-2b4d-4c5e-8f6a-1b2c3d4e5f60");

	private final TraceIdFilter filter = new TraceIdFilter(() -> FIXED_ID);

	@Test
	void setsTheTraceIdHeaderFromTheNewId() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		filter.doFilter(new MockHttpServletRequest(), response, new MockFilterChain());

		assertThat(response.getHeader(TraceIdFilter.HEADER)).isEqualTo("7f3c9a1e2b4d");
	}

	@Test
	void exposesTheTraceIdInsideTheChainAndClearsItAfterwards() throws Exception {
		AtomicReference<String> seenInChain = new AtomicReference<>();

		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
				(request, response) -> seenInChain.set(TraceIdFilter.currentTraceId()));

		assertThat(seenInChain.get()).isEqualTo("7f3c9a1e2b4d");
		assertThat(MDC.get("traceId")).isNull();
	}
}
