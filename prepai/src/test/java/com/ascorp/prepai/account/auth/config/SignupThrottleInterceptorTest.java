package com.ascorp.prepai.account.auth.config;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.ascorp.prepai.account.auth.service.SignupThrottle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

@ExtendWith(MockitoExtension.class)
class SignupThrottleInterceptorTest {

	private static final String IP = "203.0.113.7";

	@Mock
	private SignupThrottle signupThrottle;

	@Test
	void aRegisterPostIsCountedForItsClientAddress() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", SignupThrottleInterceptor.REGISTER_PATH);
		request.setRemoteAddr(IP);

		new SignupThrottleInterceptor(signupThrottle).preHandle(request, new MockHttpServletResponse(), null);

		verify(signupThrottle).assertWithinSignupLimit(IP);
	}

	@Test
	void otherRequestsAreNotCounted() {
		MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/auth/login");
		request.setRemoteAddr(IP);

		new SignupThrottleInterceptor(signupThrottle).preHandle(request, new MockHttpServletResponse(), null);

		verifyNoInteractions(signupThrottle);
	}
}
