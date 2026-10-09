package com.ascorp.prepai.account.auth.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Attaches the sign-up throttle to the register endpoint. */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

	private final SignupThrottleInterceptor signupThrottleInterceptor;

	@Override
	public void addInterceptors(InterceptorRegistry registry) {
		registry.addInterceptor(signupThrottleInterceptor).addPathPatterns(SignupThrottleInterceptor.REGISTER_PATH);
	}
}
