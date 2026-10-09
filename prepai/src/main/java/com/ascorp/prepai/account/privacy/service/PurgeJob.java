package com.ascorp.prepai.account.privacy.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the 30-day account purge every night at 02:15 (spec 2.7). */
@Component
@RequiredArgsConstructor
public class PurgeJob {

	private static final String NIGHTLY_AT_0215 = "0 15 2 * * *";

	private final AccountDeletionService deletions;

	@Scheduled(cron = NIGHTLY_AT_0215)
	public void purgeDueAccounts() {
		deletions.purgeDueAccounts();
	}
}
