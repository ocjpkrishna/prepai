package com.ascorp.prepai.generation.validation.model;

/** The layers of spec 2.6, in the order the validator runs them. */
public enum ValidationLayer {
	PARSE,
	STRUCTURE,
	CANVAS,
	TEXT_SAFETY
}
