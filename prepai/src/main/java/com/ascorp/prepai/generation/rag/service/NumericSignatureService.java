package com.ascorp.prepai.generation.rag.service;

import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * The numeric signature of a problem (spec 6.3, step 2): every number and the unit after it, in order, for example
 * `60deg|20m/s`. A sign or a power sign (`-3`, `10^-11`) is part of the number, so `10^11` and `10^-11` differ.
 * Two problems may share a cached solution only when their signatures are identical.
 */
@Component
public class NumericSignatureService {

	/** The column is VARCHAR(500) (spec 5.1); a longer signature is not cached at all rather than cut short. */
	static final int MAX_LENGTH = 500;

	private static final Pattern NUMBER_WITH_UNIT = Pattern.compile(
			"([\\^+-]*\\d+(?:\\.\\d+)?)"
					+ "([a-zA-Z°]+(?:/[a-zA-Z0-9]+)?(?:\\^[-+]?\\d+)?)?");

	/**
	 * Takes text with its case kept (see {@link TextNormalizer#withCanonicalUnits}). Empty when the signature is too
	 * long to store.
	 */
	public Optional<String> signatureOf(String normalizedText) {
		String signature = numbersWithUnits(normalizedText).collect(Collectors.joining("|"));
		return signature.length() <= MAX_LENGTH ? Optional.of(signature) : Optional.empty();
	}

	private static Stream<String> numbersWithUnits(String text) {
		return NUMBER_WITH_UNIT.matcher(text).results()
				.map(match -> match.group(1) + (match.group(2) == null ? "" : match.group(2)));
	}
}
