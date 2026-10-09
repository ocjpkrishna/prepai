package com.ascorp.prepai.generation.validation.model;

/** Machine-readable reasons a response fails validation. The message repeats the reason for the repair prompt. */
public enum ValidationCode {
	PARSE_FAILED,
	STEP_COUNT,
	STEP_NUMBER,
	TOTAL_STEPS,
	STEP_CONTENT,
	KEY_RESULTS,
	MASTERY_OPTIONS,
	ACTION_TYPE,
	ACTION_CONFIG,
	ACTION_COUNT,
	ANIMATION_DURATION,
	COORDINATE_BOUNDS,
	DRAWING_BOUNDS,
	EQUATION_POSITION,
	NARRATION_MARKUP,
	NARRATION_LENGTH,
	LATEX_DENYLIST
}
