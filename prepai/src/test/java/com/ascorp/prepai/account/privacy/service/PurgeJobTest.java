package com.ascorp.prepai.account.privacy.service;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PurgeJobTest {

	@Mock
	private AccountDeletionService deletions;

	@Test
	void theNightlyRunPurgesDueAccounts() {
		new PurgeJob(deletions).purgeDueAccounts();

		verify(deletions).purgeDueAccounts();
	}
}
