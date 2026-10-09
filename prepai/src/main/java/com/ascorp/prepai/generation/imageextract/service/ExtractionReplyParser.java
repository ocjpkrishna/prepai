package com.ascorp.prepai.generation.imageextract.service;

import com.ascorp.prepai.generation.imageextract.model.ExtractedProblem;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * Reads the reader's JSON reply (spec 3.1.1). Any text that is not that object counts as unreadable, so a model that
 * wanders off script never reaches the student.
 */
@Component
final class ExtractionReplyParser {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	ExtractedProblem parse(String reply) {
		try {
			return JSON.readValue(jsonObject(reply), ExtractedProblem.class);
		} catch (JacksonException e) {
			return ExtractedProblem.UNREADABLE;
		}
	}

	private static String jsonObject(String reply) {
		if (reply == null) {
			return "";
		}
		int start = reply.indexOf('{');
		int end = reply.lastIndexOf('}');
		return start < 0 || end < start ? "" : reply.substring(start, end + 1);
	}
}
