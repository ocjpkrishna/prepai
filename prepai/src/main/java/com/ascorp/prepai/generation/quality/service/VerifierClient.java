package com.ascorp.prepai.generation.quality.service;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.quality.model.VerifierGrade;

/**
 * Grades a stored lesson against its problem (spec 8.1, steps 3 and 4). The only implementation calls Claude Fable 5.1
 * and is active in production; no test or build step calls it.
 */
public interface VerifierClient {

	VerifierGrade grade(String problemText, LessonResponse lesson);
}
