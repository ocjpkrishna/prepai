package com.ascorp.prepai.account.auth.repository;

import com.ascorp.prepai.account.auth.model.entity.VerificationToken;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VerificationTokenRepository extends JpaRepository<VerificationToken, UUID> {

	Optional<VerificationToken> findByTokenHashAndType(String tokenHash, VerificationToken.Type type);

	void deleteByUserId(UUID userId);
}
