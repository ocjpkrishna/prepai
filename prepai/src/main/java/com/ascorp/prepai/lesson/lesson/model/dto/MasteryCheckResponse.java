package com.ascorp.prepai.lesson.lesson.model.dto;

/** Whether the pick was right, with the lesson's explanation (spec 4.2). */
public record MasteryCheckResponse(boolean correct, String explanation) {
}
