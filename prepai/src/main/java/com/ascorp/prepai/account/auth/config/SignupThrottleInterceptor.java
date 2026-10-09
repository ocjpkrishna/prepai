package com.ascorp.prepai.account.auth.config;

import com.ascorp.prepai.account.auth.service.SignupThrottle;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Counts every sign-up attempt before its body is validated, so malformed requests from one address count too.
 * Registered for the register path only (see {@link WebConfig}).
 */
@Component
@RequiredArgsConstructor
public class SignupThrottleInterceptor implements HandlerInterceptor {

	static final String REGISTER_PATH = "/api/v1/auth/register";

	private final SignupThrottle signupThrottle;

	@Override
	public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
		if (isRegistration(request)) {
			signupThrottle.assertWithinSignupLimit(request.getRemoteAddr());
		}
		return true;
	}

	private static boolean isRegistration(HttpServletRequest request) {
		return "POST".equals(request.getMethod()) && REGISTER_PATH.equals(request.getRequestURI());
	}
}
