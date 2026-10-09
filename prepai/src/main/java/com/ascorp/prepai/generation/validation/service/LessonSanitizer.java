package com.ascorp.prepai.generation.validation.service;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

/**
 * Strips HTML and script from every string of a lesson, including the free-form canvas config (spec 2.6). It runs
 * before the rules, so they check the text the student will actually see and hear.
 */
@Service
public class LessonSanitizer {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	/** Works on the JSON tree of the lesson, so every string is reached, however deep it sits. */
	public LessonResponse sanitize(LessonResponse lesson) {
		Object tree = JSON.convertValue(lesson, Map.class);
		return JSON.convertValue(clean(tree), LessonResponse.class);
	}

	private Object clean(Object node) {
		return switch (node) {
			case String text -> stripHtml(text);
			case Map<?, ?> map -> cleanMap(map);
			case List<?> list -> list.stream().map(this::clean).toList();
			case null, default -> node;
		};
	}

	private Map<String, Object> cleanMap(Map<?, ?> map) {
		Map<String, Object> cleaned = new LinkedHashMap<>();
		map.forEach((key, value) -> cleaned.put(String.valueOf(key), clean(value)));
		return cleaned;
	}

	private String stripHtml(String text) {
		return Parser.unescapeEntities(Jsoup.clean(text, Safelist.none()), false);
	}
}
