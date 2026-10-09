package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.errors.ApiException;
import com.ascorp.prepai.common.errors.ErrorCode;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.common.model.lesson.LessonResponse.MasteryCheck;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Option;
import com.ascorp.prepai.lesson.lesson.model.dto.MasteryCheckResponse;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** Marks the student's answer to a lesson's mastery check (spec 4.2). */
@Service
@RequiredArgsConstructor
public class MasteryCheckService {

	private final LessonHistoryService history;

	public MasteryCheckResponse answer(UUID userId, UUID lessonId, String selectedOptionId) {
		MasteryCheck check = masteryCheckOf(history.get(userId, lessonId));
		Option picked = check.options().stream()
				.filter(option -> option.id().equals(selectedOptionId))
				.findFirst()
				.orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED, "Choose one of the options."));
		return new MasteryCheckResponse(picked.correct(), check.explanation());
	}

	private static MasteryCheck masteryCheckOf(LessonResponse lesson) {
		if (lesson.masteryCheck() == null || lesson.masteryCheck().options() == null) {
			throw new ApiException(ErrorCode.NOT_FOUND, "This lesson has no mastery check.");
		}
		return lesson.masteryCheck();
	}
}
