package com.ascorp.prepai.lesson.lesson.model.dto;

import java.util.List;

/** A page of the lesson history (spec 4.2), newest first. */
public record LessonHistoryPage(List<LessonSummaryDto> items, int page, int size, long totalElements,
		int totalPages) {
}
