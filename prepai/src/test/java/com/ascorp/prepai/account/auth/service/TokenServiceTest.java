package com.ascorp.prepai.account.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.account.auth.config.AuthProperties;
import com.ascorp.prepai.account.auth.model.dto.TokenResponse;
import com.ascorp.prepai.account.auth.model.entity.RefreshToken;
import com.ascorp.prepai.account.auth.repository.RefreshTokenRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

	private static final Instant NOW = Instant.parse("2026-10-08T10:00:00Z");
	private static final UUID USER = UUID.fromString("0b3f1c2e-5d6a-4e7b-8c9d-0e1f2a3b4c5d");
	private static final UUID FAMILY = UUID.fromString("1c4f2d3e-6e7b-4f8c-9d0e-1f2a3b4c5d6e");
	private static final UUID NEW_ID = UUID.fromString("2d5f3e4f-7f8c-4a9d-8e1f-2a3b4c5d6e7f");
	private static final String RAW_REFRESH = "raw-refresh-token";
	private static final String ACCESS = "access-token";
	private static final int ACCESS_HOURS = 1;
	private static final int REFRESH_DAYS = 30;

	@Mock
	private RefreshTokenRepository refreshTokens;

	@Mock
	private AccessTokenEncoder accessTokens;

	private final OpaqueTokens opaqueTokens = new OpaqueTokens();
	private TokenService tokenService;

	@BeforeEach
	void setUp() {
		AuthProperties properties = new AuthProperties(
				new AuthProperties.Jwt("test-secret", ACCESS_HOURS, REFRESH_DAYS), new AuthProperties.Google("client"));
		Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
		tokenService = new TokenService(refreshTokens, opaqueTokens, accessTokens, properties, clock, () -> NEW_ID);
	}

	@Test
	void issueReturnsTokensAndStoresOnlyTheHashOfTheRefreshToken() {
		when(accessTokens.encode(USER)).thenReturn(ACCESS);

		TokenResponse response = tokenService.issue(USER);

		ArgumentCaptor<RefreshToken> saved = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokens).save(saved.capture());
		assertThat(response.accessToken()).isEqualTo(ACCESS);
		assertThat(response.expiresIn()).isEqualTo(Duration.ofHours(ACCESS_HOURS).toSeconds());
		assertThat(saved.getValue().getTokenHash()).isEqualTo(opaqueTokens.hash(response.refreshToken()));
		assertThat(saved.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(REFRESH_DAYS)));
	}

	@Test
	void rotateRevokesTheUsedTokenAndIssuesANewOneInTheSameFamily() {
		RefreshToken used = storedToken(FAMILY, null, NOW.plus(Duration.ofDays(1)));
		givenStored(used);
		when(refreshTokens.revokeIfActive(NEW_ID, NOW)).thenReturn(1);
		when(accessTokens.encode(USER)).thenReturn(ACCESS);

		TokenResponse response = tokenService.rotate(RAW_REFRESH);

		ArgumentCaptor<RefreshToken> issued = ArgumentCaptor.forClass(RefreshToken.class);
		verify(refreshTokens).save(issued.capture());
		assertThat(issued.getValue().getFamilyId()).isEqualTo(FAMILY);
		assertThat(response.refreshToken()).isNotEqualTo(RAW_REFRESH);
	}

	@Test
	void rotateRejectsAReusedTokenAndRevokesItsWholeFamily() {
		givenStored(storedToken(FAMILY, NOW.minus(Duration.ofMinutes(1)), NOW.plus(Duration.ofDays(1))));

		assertThatThrownBy(() -> tokenService.rotate(RAW_REFRESH))
				.isInstanceOfSatisfying(ApiException.class,
						exception -> assertThat(exception.getCode()).isEqualTo(ErrorCode.UNAUTHENTICATED));
		verify(refreshTokens).revokeFamily(FAMILY, NOW);
	}

	@Test
	void rotateRejectsATokenThatAParallelRequestAlreadyRotated() {
		givenStored(storedToken(FAMILY, null, NOW.plus(Duration.ofDays(1))));
		when(refreshTokens.revokeIfActive(NEW_ID, NOW)).thenReturn(0);

		assertThatThrownBy(() -> tokenService.rotate(RAW_REFRESH)).isInstanceOf(ApiException.class);
		verify(refreshTokens).revokeFamily(FAMILY, NOW);
		verify(refreshTokens, never()).save(any());
	}

	@Test
	void rotateRejectsAnExpiredTokenWithoutRevokingAnything() {
		givenStored(storedToken(FAMILY, null, NOW));

		assertThatThrownBy(() -> tokenService.rotate(RAW_REFRESH)).isInstanceOf(ApiException.class);
		verify(refreshTokens, never()).revokeFamily(any(), any());
	}

	@Test
	void rotateRejectsAnUnknownToken() {
		when(refreshTokens.findByTokenHash(opaqueTokens.hash(RAW_REFRESH))).thenReturn(Optional.empty());

		assertThatThrownBy(() -> tokenService.rotate(RAW_REFRESH)).isInstanceOf(ApiException.class);
	}

	private void givenStored(RefreshToken stored) {
		when(refreshTokens.findByTokenHash(opaqueTokens.hash(RAW_REFRESH))).thenReturn(Optional.of(stored));
	}

	private RefreshToken storedToken(UUID family, Instant revokedAt, Instant expiresAt) {
		return RefreshToken.builder()
				.id(NEW_ID)
				.userId(USER)
				.familyId(family)
				.tokenHash(opaqueTokens.hash(RAW_REFRESH))
				.expiresAt(expiresAt)
				.revokedAt(revokedAt)
				.build();
	}
}
