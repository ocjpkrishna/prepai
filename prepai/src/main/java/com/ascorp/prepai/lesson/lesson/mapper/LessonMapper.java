package com.ascorp.prepai.lesson.lesson.mapper;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.lesson.lesson.model.dto.LessonSummaryDto;
import com.ascorp.prepai.lesson.lesson.model.entity.Lesson;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** Converts between the stored lesson row, its JSON and the API models. */
@Component
public class LessonMapper {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	public LessonResponse toResponse(Lesson lesson) {
		return JSON.readValue(lesson.getResponseJson(), LessonResponse.class);
	}

	public String toJson(LessonResponse response) {
		return JSON.writeValueAsString(response);
	}

	public LessonSummaryDto toSummary(Lesson lesson) {
		return new LessonSummaryDto(lesson.getId(), lesson.getTitle(), lesson.getSubject(), lesson.getTopic(),
				lesson.getExam(), lesson.getDifficulty(), lesson.getRating(), lesson.getCreatedAt());
	}
}
