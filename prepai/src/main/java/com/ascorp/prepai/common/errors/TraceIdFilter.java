package com.ascorp.prepai.common.errors;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gives every request a short trace ID, put in the MDC for the logs and in the X-Trace-Id header, so a student
 * can quote it to support. It runs first, so even 401 responses from the security layer carry it.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class TraceIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Trace-Id";
	private static final String MDC_KEY = "traceId";
	private static final int TRACE_ID_LENGTH = 12;

	private final Supplier<UUID> idSupplier;

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String traceId = newTraceId();
		MDC.put(MDC_KEY, traceId);
		response.setHeader(HEADER, traceId);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(MDC_KEY);
		}
	}

	/** The trace ID of the request being handled, or null outside a request. */
	public static String currentTraceId() {
		return MDC.get(MDC_KEY);
	}

	private String newTraceId() {
		return idSupplier.get().toString().replace("-", "").substring(0, TRACE_ID_LENGTH);
	}
}
