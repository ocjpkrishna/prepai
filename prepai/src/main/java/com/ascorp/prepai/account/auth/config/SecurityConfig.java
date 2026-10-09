package com.ascorp.prepai.account.auth.config;

import com.ascorp.prepai.common.config.AppProperties;
import com.ascorp.prepai.common.errors.ForbiddenAccessDeniedHandler;
import com.ascorp.prepai.common.errors.TraceIdFilter;
import com.ascorp.prepai.common.errors.UnauthenticatedEntryPoint;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/** Stateless bearer-token security: everything needs a token except the public endpoints (spec 9.2, Agent 2 task 5). */
@Configuration
@RequiredArgsConstructor
public class SecurityConfig {

	private static final String[] PUBLIC_PATHS = {"/api/v1/auth/**", "/api/v1/subscriptions/plans",
			"/api/v1/subscriptions/webhook", "/error"};

	private final AppProperties app;
	private final UnauthenticatedEntryPoint unauthenticatedEntryPoint;
	private final ForbiddenAccessDeniedHandler forbiddenAccessDeniedHandler;

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http
				.csrf(csrf -> csrf.disable())
				.cors(Customizer.withDefaults())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.authorizeHttpRequests(requests -> requests
						.requestMatchers(PUBLIC_PATHS).permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
						.authenticationEntryPoint(unauthenticatedEntryPoint))
				.exceptionHandling(errors -> errors
						.authenticationEntryPoint(unauthenticatedEntryPoint)
						.accessDeniedHandler(forbiddenAccessDeniedHandler))
				.build();
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration cors = new CorsConfiguration();
		cors.setAllowedOrigins(app.corsAllowedOrigins());
		cors.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
		cors.setAllowedHeaders(List.of("Authorization", "Content-Type"));
		cors.setExposedHeaders(List.of(TraceIdFilter.HEADER));
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", cors);
		return source;
	}

	@Bean
	public PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}
}
