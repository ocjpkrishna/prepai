package com.ascorp.prepai.account.auth.repository;

import com.ascorp.prepai.account.auth.model.entity.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, UUID> {

	Optional<User> findByEmail(String email);

	Optional<User> findByGoogleId(String googleId);

	List<User> findByDeletionRequestedAtBeforeAndPurgedAtIsNull(Instant cutoff);
}
