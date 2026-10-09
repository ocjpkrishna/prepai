package com.ascorp.prepai.lesson.lesson.model.dto;

import com.ascorp.prepai.generation.imageextract.model.Confidence;

/** The text read from a photo, for the student to confirm or edit (spec 4.2). */
public record ExtractResponse(String problemText, Confidence confidence, boolean hasDiagram) {
}
