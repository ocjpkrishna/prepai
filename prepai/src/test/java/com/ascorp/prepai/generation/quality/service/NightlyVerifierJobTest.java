package com.ascorp.prepai.generation.quality.service;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ascorp.prepai.generation.quality.model.QualityProperties;
import com.ascorp.prepai.generation.quality.model.VerificationReport;
import org.junit.jupiter.api.Test;

class NightlyVerifierJobTest {

	private final VerificationService verification = mock(VerificationService.class);
	private final NightlyVerifierJob job = new NightlyVerifierJob(verification,
			new QualityProperties(50, "claude-fable-5-1"));

	@Test
	void runsOneBatchOfTheConfiguredSize() {
		when(verification.verifyBatch(anyInt())).thenReturn(new VerificationReport(40, 8, 2));

		job.verifyBatch();

		verify(verification).verifyBatch(50);
	}
}
