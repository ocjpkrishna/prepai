package com.ascorp.prepai.generation.quality.service;

import com.ascorp.prepai.generation.quality.model.QualityProperties;
import com.ascorp.prepai.generation.quality.model.VerificationReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Runs the verifier batch every night at 02:30, after the account purge (spec 8.1). Production only. */
@Slf4j
@Component
@Profile("prod")
@RequiredArgsConstructor
public class NightlyVerifierJob {

	private static final String NIGHTLY_AT_0230 = "0 30 2 * * *";

	private final VerificationService verification;
	private final QualityProperties properties;

	@Scheduled(cron = NIGHTLY_AT_0230)
	public void verifyBatch() {
		VerificationReport report = verification.verifyBatch(properties.batchSize());
		log.info("Nightly verifier: {} verified, {} corrected, {} failed", report.verified(), report.corrected(),
				report.failed());
	}
}
