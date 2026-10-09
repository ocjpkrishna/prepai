package com.ascorp.prepai.common.errors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

/** Answers a missing or expired token with the 401 shape of spec 4.7. */
@Component
@RequiredArgsConstructor
public class UnauthenticatedEntryPoint implements AuthenticationEntryPoint {

	private final ErrorResponseWriter errorResponseWriter;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException exception)
			throws IOException {
		errorResponseWriter.write(response, ErrorCode.UNAUTHENTICATED);
	}
}
