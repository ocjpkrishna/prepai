package com.ascorp.prepai.generation.quality.model;

/**
 * The verifier's grade of one lesson (spec 8.1, step 4). `errorType` names the kind of mistake when the answer is
 * wrong, and is null otherwise.
 */
public record VerifierGrade(boolean correct, int stepQuality, int teachingClarity, String errorType) {

	/** Below this step quality a lesson is corrected even when its final answer is right (spec 8.1, step 5). */
	private static final int PASSING_STEP_QUALITY = 3;

	public boolean passes() {
		return correct && stepQuality >= PASSING_STEP_QUALITY;
	}
}
