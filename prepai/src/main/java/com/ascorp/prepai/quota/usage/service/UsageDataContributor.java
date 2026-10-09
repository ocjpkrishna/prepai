package com.ascorp.prepai.quota.usage.service;

import com.ascorp.prepai.common.model.PersonalDataContributor;
import com.ascorp.prepai.quota.usage.model.dto.UsageEntryResponse;
import com.ascorp.prepai.quota.usage.repository.UsageLogRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Puts the student's usage in the data download and erases it on purge (spec 2.7). */
@Component
@RequiredArgsConstructor
public class UsageDataContributor implements PersonalDataContributor {

	private static final String SECTION = "usage";

	private final UsageLogRepository usageLogs;

	@Override
	public String section() {
		return SECTION;
	}

	@Override
	@Transactional(readOnly = true)
	public Object export(UUID userId) {
		return usageLogs.findByUserIdOrderByCreatedAtAsc(userId).stream()
				.map(UsageEntryResponse::from)
				.toList();
	}

	@Override
	@Transactional
	public void erase(UUID userId) {
		usageLogs.deleteByUserId(userId);
	}
}
