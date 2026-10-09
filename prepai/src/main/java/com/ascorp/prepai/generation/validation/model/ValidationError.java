package com.ascorp.prepai.generation.validation.model;

/** One reason a response failed validation: its layer, a machine-readable code and a sentence to repair from. */
public record ValidationError(ValidationLayer layer, ValidationCode code, String message) {
}
