package com.ascorp.prepai.support;

import com.ascorp.prepai.account.auth.mail.VerificationMailer;
import com.ascorp.prepai.account.auth.repository.RefreshTokenRepository;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.repository.VerificationTokenRepository;
import com.ascorp.prepai.billing.subscription.repository.ProcessedWebhookEventRepository;
import com.ascorp.prepai.billing.subscription.repository.SubscriptionRepository;
import com.ascorp.prepai.common.errors.TraceIdFilter;
import com.ascorp.prepai.common.redis.RedisCounter;
import com.ascorp.prepai.generation.cache.repository.LessonCacheRepository;
import com.ascorp.prepai.generation.rag.repository.ProblemEmbeddingRepository;
import com.ascorp.prepai.lesson.lesson.repository.LessonRepository;
import com.ascorp.prepai.quota.usage.repository.UsageLogRepository;
import jakarta.servlet.Filter;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * The full web stack on the test profile, with every repository and external client mocked, so the suites
 * that extend it run without PostgreSQL or Redis. Subclasses add the @SpringBootTest annotation.
 */
public abstract class ApiTestSupport {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private TraceIdFilter traceIdFilter;

	@Autowired
	private JwtEncoder jwtEncoder;

	@MockitoBean
	protected UserRepository users;

	@MockitoBean
	protected RefreshTokenRepository refreshTokens;

	@MockitoBean
	protected VerificationTokenRepository verificationTokens;

	@MockitoBean
	protected VerificationMailer verificationMailer;

	@MockitoBean
	protected RedisCounter redisCounter;

	@MockitoBean
	protected UsageLogRepository usageLogs;

	@MockitoBean
	protected LessonRepository lessons;

	@MockitoBean
	protected SubscriptionRepository subscriptions;

	@MockitoBean
	protected ProcessedWebhookEventRepository processedWebhookEvents;

	@MockitoBean
	protected EmbeddingModel embeddingModel;

	@MockitoBean
	protected ProblemEmbeddingRepository problemEmbeddings;

	@MockitoBean
	protected LessonCacheRepository lessonCache;

	protected MockMvc mvc;

	@BeforeEach
	void setUpMockMvc() {
		Filter securityChain = context.getBean("springSecurityFilterChain", Filter.class);
		mvc = MockMvcBuilders.webAppContextSetup(context)
				.addFilter(traceIdFilter, "/*")
				.addFilter(securityChain, "/*")
				.build();
	}

	/** A token signed with the app's own key, valid for one minute from now. */
	protected String signedToken() {
		Instant now = Instant.now();
		return signedToken(UUID.randomUUID(), now, now.plusSeconds(60));
	}

	protected String signedToken(UUID subject, Instant issuedAt, Instant expiresAt) {
		JwtClaimsSet claims = JwtClaimsSet.builder()
				.subject(subject.toString())
				.issuedAt(issuedAt)
				.expiresAt(expiresAt)
				.build();
		JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
		return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
	}
}
