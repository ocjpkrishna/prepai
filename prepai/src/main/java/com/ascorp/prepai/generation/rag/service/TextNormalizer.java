package com.ascorp.prepai.generation.rag.service;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Prepares problem text for the RAG cache (spec 6.3, step 1): obvious emails and Indian mobile numbers are redacted
 * (spec 6.3, ingestion), typographic characters become plain ones (a real minus sign, superscript digits, full-width
 * digits), then the text is lowercased, whitespace is collapsed and units after a number are canonical.
 */
@Component
public class TextNormalizer {

	private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
	private static final Pattern MOBILE = Pattern.compile(
			"(?<![\\d.])(\\+?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}(?![\\d.])");
	private static final Pattern MINUS_LOOKALIKES = Pattern.compile("[\u2212\u2012\u2013]");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern NUMBER_UNIT = Pattern.compile("(\\d)\\s*([a-zA-Z°]+)");
	private static final Map<String, String> UNITS = Map.ofEntries(
			Map.entry("degrees", "deg"), Map.entry("degree", "deg"), Map.entry("°", "deg"),
			Map.entry("meters", "m"), Map.entry("meter", "m"), Map.entry("metres", "m"), Map.entry("metre", "m"),
			Map.entry("seconds", "s"), Map.entry("second", "s"), Map.entry("sec", "s"),
			Map.entry("kilograms", "kg"), Map.entry("kilogram", "kg"), Map.entry("grams", "g"), Map.entry("gram", "g"),
			Map.entry("newtons", "N"), Map.entry("newton", "N"), Map.entry("joules", "J"), Map.entry("joule", "J"),
			Map.entry("watts", "W"), Map.entry("watt", "W"), Map.entry("volts", "V"), Map.entry("volt", "V"),
			Map.entry("amperes", "A"), Map.entry("ampere", "A"), Map.entry("amps", "A"), Map.entry("amp", "A"),
			Map.entry("ohms", "ohm"), Map.entry("hours", "h"), Map.entry("hour", "h"),
			Map.entry("minutes", "min"), Map.entry("minute", "min"));

	/** Lowercased, for embedding and storage. */
	public String normalize(String text) {
		return withCanonicalUnits(text).toLowerCase(Locale.ROOT);
	}

	/**
	 * The same text with the case kept, for the numeric signature: SI prefixes differ only in case (`5 mW` is not
	 * `5 MW`), so the signature must not be built from lowercased text.
	 */
	public String withCanonicalUnits(String text) {
		String plain = MINUS_LOOKALIKES.matcher(Normalizer.normalize(text, Normalizer.Form.NFKC)).replaceAll("-");
		String redacted = MOBILE.matcher(EMAIL.matcher(plain).replaceAll("[email]")).replaceAll("[phone]");
		String collapsed = WHITESPACE.matcher(redacted.strip()).replaceAll(" ");
		return NUMBER_UNIT.matcher(collapsed).replaceAll(match -> match.group(1) + canonicalUnit(match.group(2)));
	}

	private static String canonicalUnit(String unit) {
		return UNITS.getOrDefault(unit.toLowerCase(Locale.ROOT), unit);
	}
}
