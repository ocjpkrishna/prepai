package com.ascorp.prepai.account.auth.service;

import com.ascorp.prepai.account.auth.config.AuthProperties;
import com.ascorp.prepai.account.auth.model.dto.TokenResponse;
import com.ascorp.prepai.account.auth.model.entity.RefreshToken;
import com.ascorp.prepai.account.auth.repository.RefreshTokenRepository;
import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import java.time.Clock;
import java.util.UUID;
import java.util.function.Supplier;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Issues the tokens of a login and rotates refresh tokens (spec 4.1). */
@Service
@RequiredArgsConstructor
public class TokenService {

	private final RefreshTokenRepository refreshTokens;
	private final OpaqueTokens opaqueTokens;
	private final AccessTokenEncoder accessTokens;
	private final AuthProperties properties;
	private final Clock clock;
	private final Supplier<UUID> ids;

	/** Starts a login with a new token family, so a stolen token can be revoked with all its relatives. */
	@Transactional
	public TokenResponse issue(UUID userId) {
		return issueInFamily(userId, ids.get());
	}

	/**
	 * Every refresh token works once. Reusing a rotated one revokes its whole family. The reuse error must not
	 * roll that revocation back, hence noRollbackFor.
	 */
	@Transactional(noRollbackFor = ApiException.class)
	public TokenResponse rotate(String rawRefreshToken) {
		RefreshToken current = findUsable(rawRefreshToken);
		current.revoke(clock.instant());
		return issueInFamily(current.getUserId(), current.getFamilyId());
	}

	private RefreshToken findUsable(String rawRefreshToken) {
		RefreshToken token = refreshTokens.findByTokenHash(opaqueTokens.hash(rawRefreshToken))
				.orElseThrow(TokenService::sessionEnded);
		if (token.isRevoked()) {
			refreshTokens.revokeFamily(token.getFamilyId(), clock.instant());
			throw sessionEnded();
		}
		if (token.isExpired(clock.instant())) {
			throw sessionEnded();
		}
		return token;
	}

	private TokenResponse issueInFamily(UUID userId, UUID familyId) {
		String refreshToken = opaqueTokens.newToken();
		refreshTokens.save(RefreshToken.builder()
				.id(ids.get())
				.userId(userId)
				.familyId(familyId)
				.tokenHash(opaqueTokens.hash(refreshToken))
				.expiresAt(clock.instant().plus(properties.jwt().refreshLifetime()))
				.build());
		long expiresIn = properties.jwt().accessLifetime().toSeconds();
		return new TokenResponse(accessTokens.encode(userId), refreshToken, expiresIn);
	}

	private static ApiException sessionEnded() {
		return new ApiException(ErrorCode.UNAUTHENTICATED, "Your session has ended. Please log in again.");
	}
}
