package com.ascorp.prepai.lesson.lesson.model.dto;

import jakarta.validation.constraints.NotBlank;

/** The option the student picked (spec 4.2). */
public record MasteryCheckRequest(@NotBlank String selectedOptionId) {
}
