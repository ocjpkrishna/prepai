package com.ascorp.prepai;

import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.account.auth.mail.VerificationMailer;
import com.ascorp.prepai.account.auth.repository.RefreshTokenRepository;
import com.ascorp.prepai.account.auth.repository.UserRepository;
import com.ascorp.prepai.account.auth.repository.VerificationTokenRepository;
import com.ascorp.prepai.common.redis.RedisCounter;
import com.ascorp.prepai.billing.subscription.repository.ProcessedWebhookEventRepository;
import com.ascorp.prepai.billing.subscription.repository.SubscriptionRepository;
import com.ascorp.prepai.generation.cache.repository.LessonCacheRepository;
import com.ascorp.prepai.generation.rag.repository.ProblemEmbeddingRepository;
import com.ascorp.prepai.lesson.lesson.repository.LessonRepository;
import com.ascorp.prepai.quota.usage.repository.UsageLogRepository;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** The `test` profile has no database, so the repositories are replaced by mocks; the wiring is still real. */
@SpringBootTest
@ActiveProfiles("test")
class PrepaiApplicationTests {

	private final Clock clock;

	@MockitoBean
	UserRepository users;

	@MockitoBean
	RefreshTokenRepository refreshTokens;

	@MockitoBean
	VerificationTokenRepository verificationTokens;

	@MockitoBean
	VerificationMailer verificationMailer;

	@MockitoBean
	RedisCounter redisCounter;

	@MockitoBean
	UsageLogRepository usageLogs;

	@MockitoBean
	LessonRepository lessonRepository;

	@MockitoBean
	SubscriptionRepository subscriptions;

	@MockitoBean
	ProcessedWebhookEventRepository processedWebhookEvents;

	@MockitoBean
	EmbeddingModel embeddingModel;

	@MockitoBean
	ProblemEmbeddingRepository problemEmbeddings;

	@MockitoBean
	LessonCacheRepository lessonCache;

	@Autowired
	PrepaiApplicationTests(Clock clock) {
		this.clock = clock;
	}

	@Test
	void contextStartsWithTheSharedBeans() {
		assertThat(clock.getZone().getId()).isEqualTo("Z");
	}
}
