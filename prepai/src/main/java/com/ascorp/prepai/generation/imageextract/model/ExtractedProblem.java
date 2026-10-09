package com.ascorp.prepai.generation.imageextract.model;

/**
 * The problem text read from one photo (spec 3.1.1). Diagrams are described in words inside `problemText`.
 * Only a readable result may reach the student; a LOW reading or an empty text is unreadable.
 */
public record ExtractedProblem(String problemText, Confidence confidence, boolean hasDiagram) {

	public static final ExtractedProblem UNREADABLE = new ExtractedProblem("", Confidence.LOW, false);

	public boolean readable() {
		return confidence != null && confidence != Confidence.LOW && problemText != null && !problemText.isBlank();
	}
}
