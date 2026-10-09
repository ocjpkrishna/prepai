package com.ascorp.prepai.generation.rag.model;

import com.ascorp.prepai.common.model.enums.Difficulty;
import com.ascorp.prepai.common.model.enums.Exam;
import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Subject;
import com.ascorp.prepai.common.model.lesson.LessonRequest;
import com.ascorp.prepai.common.model.lesson.LessonResponse;
import java.util.UUID;

/** A stored problem and its solution, as the nightly verifier sees it (spec 8.1). */
public record StoredProblem(UUID id, String subject, String exam, String problemText, LessonResponse lesson) {

	/** The request that regenerates this problem when the verifier corrects it (spec 8.1, step 5b). */
	public LessonRequest toRequest() {
		return new LessonRequest(LessonRequest.Type.PROBLEM, Subject.valueOf(subject), Exam.valueOf(exam),
				new LessonRequest.Input(problemText, null), Difficulty.MEDIUM, Language.EN);
	}
}
