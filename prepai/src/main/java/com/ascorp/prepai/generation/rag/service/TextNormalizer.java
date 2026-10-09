package com.ascorp.prepai.generation.rag.service;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Prepares problem text for the RAG cache (spec 6.3, step 1): obvious emails and Indian mobile numbers are redacted
 * (spec 6.3, ingestion), then the text is lowercased, whitespace is collapsed and units after a number are canonical.
 */
@Component
public class TextNormalizer {

	private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+");
	private static final Pattern MOBILE = Pattern.compile(
			"(?<![\\d.])(\\+?91[\\s-]?)?[6-9]\\d{4}[\\s-]?\\d{5}(?![\\d.])");
	private static final Pattern WHITESPACE = Pattern.compile("\\s+");
	private static final Pattern NUMBER_UNIT = Pattern.compile("(\\d)\\s*([a-z°]+)");
	private static final Map<String, String> UNITS = Map.ofEntries(
			Map.entry("degrees", "deg"), Map.entry("degree", "deg"), Map.entry("°", "deg"),
			Map.entry("meters", "m"), Map.entry("meter", "m"), Map.entry("metres", "m"), Map.entry("metre", "m"),
			Map.entry("seconds", "s"), Map.entry("second", "s"), Map.entry("sec", "s"),
			Map.entry("kilograms", "kg"), Map.entry("kilogram", "kg"), Map.entry("grams", "g"), Map.entry("gram", "g"),
			Map.entry("newtons", "n"), Map.entry("newton", "n"), Map.entry("joules", "j"), Map.entry("joule", "j"),
			Map.entry("watts", "w"), Map.entry("watt", "w"), Map.entry("volts", "v"), Map.entry("volt", "v"),
			Map.entry("amperes", "a"), Map.entry("ampere", "a"), Map.entry("amps", "a"), Map.entry("amp", "a"),
			Map.entry("ohms", "ohm"), Map.entry("hours", "h"), Map.entry("hour", "h"),
			Map.entry("minutes", "min"), Map.entry("minute", "min"));

	public String normalize(String text) {
		String redacted = MOBILE.matcher(EMAIL.matcher(text).replaceAll("[email]")).replaceAll("[phone]");
		String collapsed = WHITESPACE.matcher(redacted.toLowerCase(Locale.ROOT).strip()).replaceAll(" ");
		return NUMBER_UNIT.matcher(collapsed).replaceAll(match -> match.group(1) + canonicalUnit(match.group(2)));
	}

	private static String canonicalUnit(String unit) {
		return UNITS.getOrDefault(unit, unit);
	}
}
