package com.ascorp.prepai.quota.usage.repository;

import com.ascorp.prepai.quota.usage.model.entity.UsageLog;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsageLogRepository extends JpaRepository<UsageLog, UUID> {

	long countByUserIdAndSessionDate(UUID userId, LocalDate sessionDate);

	List<UsageLog> findByUserIdOrderByCreatedAtAsc(UUID userId);

	void deleteByUserId(UUID userId);
}
