package com.ascorp.prepai.generation.llm.prompt;

import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Removes every form of the {@code <problem>} tag from untrusted text (spec 2.6, prompt injection), so that text cannot
 * close the data block early. Case, spaces inside the tag and tags rebuilt from the pieces left by a removal
 * ({@code </prob</problem>lem>}) are all handled by removing until nothing is left.
 */
public final class ProblemTags {

	private static final Pattern TAG = Pattern.compile("<\\s*/?\\s*problem\\b[^>]*>?", Pattern.CASE_INSENSITIVE);

	private ProblemTags() {
	}

	public static String strip(String text) {
		String current = Objects.toString(text, "");
		String stripped = TAG.matcher(current).replaceAll("");
		return stripped.equals(current) ? stripped : strip(stripped);
	}
}
