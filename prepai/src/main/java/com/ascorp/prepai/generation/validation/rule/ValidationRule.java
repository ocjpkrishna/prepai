package com.ascorp.prepai.generation.validation.rule;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import java.util.List;

/**
 * One check on a parsed and sanitised lesson (spec 2.6). Spring collects every bean of this type, so a new check
 * is a new bean and the validator does not change.
 */
public interface ValidationRule {

	List<ValidationError> check(LessonResponse lesson);
}
