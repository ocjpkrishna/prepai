package com.ascorp.prepai.generation.quality.model;

/** What one nightly batch did, counted per outcome; a failed row stays unverified and is tried again the next night. */
public record VerificationReport(int verified, int corrected, int failed) {

	public enum Outcome {
		VERIFIED,
		CORRECTED,
		FAILED
	}

	public static VerificationReport empty() {
		return new VerificationReport(0, 0, 0);
	}

	public VerificationReport record(Outcome outcome) {
		return switch (outcome) {
			case VERIFIED -> new VerificationReport(verified + 1, corrected, failed);
			case CORRECTED -> new VerificationReport(verified, corrected + 1, failed);
			case FAILED -> new VerificationReport(verified, corrected, failed + 1);
		};
	}
}
