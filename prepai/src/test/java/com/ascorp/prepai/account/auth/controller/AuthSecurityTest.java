package com.ascorp.prepai.account.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.ascorp.prepai.account.auth.mail.VerificationMailer;
import com.ascorp.prepai.account.auth.repository.RefreshTokenRepository;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.repository.VerificationTokenRepository;
import com.ascorp.prepai.common.errors.TraceIdFilter;
import com.ascorp.prepai.common.redis.RedisCounter;
import com.ascorp.prepai.billing.subscription.repository.ProcessedWebhookEventRepository;
import com.ascorp.prepai.billing.subscription.repository.SubscriptionRepository;
import com.ascorp.prepai.generation.cache.repository.LessonCacheRepository;
import com.ascorp.prepai.generation.rag.repository.ProblemEmbeddingRepository;
import com.ascorp.prepai.quota.usage.repository.UsageLogRepository;
import jakarta.servlet.Filter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** The security layer over HTTP: public auth endpoints, bearer tokens, and the 4.7 error shape on 401. */
@SpringBootTest
@ActiveProfiles("test")
class AuthSecurityTest {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private TraceIdFilter traceIdFilter;

	@Autowired
	private JwtEncoder jwtEncoder;

	@MockitoBean
	private UserRepository users;

	@MockitoBean
	private RefreshTokenRepository refreshTokens;

	@MockitoBean
	private VerificationTokenRepository verificationTokens;

	@MockitoBean
	private VerificationMailer verificationMailer;

	@MockitoBean
	private RedisCounter redisCounter;

	@MockitoBean
	private UsageLogRepository usageLogs;

	@MockitoBean
	private SubscriptionRepository subscriptions;

	@MockitoBean
	private ProcessedWebhookEventRepository processedWebhookEvents;

	@MockitoBean
	private EmbeddingModel embeddingModel;

	@MockitoBean
	private ProblemEmbeddingRepository problemEmbeddings;

	@MockitoBean
	private LessonCacheRepository lessonCache;

	private MockMvc mvc;

	@BeforeEach
	void setUp() {
		Filter securityChain = context.getBean("springSecurityFilterChain", Filter.class);
		mvc = MockMvcBuilders.webAppContextSetup(context)
				.addFilter(traceIdFilter, "/*")
				.addFilter(securityChain, "/*")
				.build();
	}

	@Test
	void protectedEndpointWithoutTokenAnswersWith401InTheErrorShape() throws Exception {
		mvc.perform(get("/api/v1/users/me"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"))
				.andExpect(header().exists(TraceIdFilter.HEADER));
	}

	@Test
	void loginIsPublicAndAnswersWithTheServiceError() throws Exception {
		String body = "{\"email\":\"nobody@example.com\",\"password\":\"whatever-1\"}";

		mvc.perform(post("/api/v1/auth/login").contentType("application/json").content(body))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.message").value("The email or password is incorrect."));
	}

	@Test
	void validBearerTokenPassesTheSecurityLayer() throws Exception {
		mvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + signedToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
	}

	private String signedToken() {
		Instant now = Instant.now();
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(UUID.randomUUID().toString())
				.issuedAt(now)
				.expiresAt(now.plusSeconds(60))
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
