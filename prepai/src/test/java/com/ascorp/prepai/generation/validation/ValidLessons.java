package com.ascorp.prepai.generation.validation;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.common.model.lesson.LessonResponse.MasteryCheck;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Step;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Summary;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/** Lessons for the validation tests: one valid three-step lesson that passes every layer of spec 2.6. */
public final class ValidLessons {

	private static final JsonMapper JSON = JsonMapper.builder().build();

	private static final String VALID_JSON = """
			{
			 "lessonId": "3f0c6a52-6b1e-4d0c-9a57-2f8e1c1b7d10", "title": "Projectile Motion",
			 "subject": "PHYSICS", "topic": "Kinematics", "difficulty": "MEDIUM",
			 "totalSteps": 3, "estimatedDurationSeconds": 180,
			 "steps": [
			  {"stepNumber": 1, "title": "Resolve velocity", "narration": "Break the velocity into components.",
			   "canvas": {"actions": [
			    {"type": "DRAW_AXIS", "animationDuration": 1000,
			     "config": {"origin": {"x": 100, "y": 300}, "xLength": 400, "yLength": 250,
			                "xLabel": "x", "yLabel": "y"}},
			    {"type": "DRAW_ARROW", "animationDuration": 800,
			     "config": {"from": {"x": 100, "y": 300}, "to": {"x": 350, "y": 100}, "label": "u",
			                "color": "#4A90D9", "angle": 60}}
			   ]},
			   "equations": [{"latex": "u_x = u * cos 60", "highlight": true, "position": {"x": 520, "y": 150}}]},
			  {"stepNumber": 2, "title": "Time of flight", "narration": "The particle returns to the same height.",
			   "canvas": {"actions": [
			    {"type": "DRAW_LINE", "animationDuration": 600,
			     "config": {"from": {"x": 100, "y": 320}, "to": {"x": 450, "y": 320},
			                "color": "#E67E22", "strokeWidth": 2}}
			   ]},
			   "equations": [{"latex": "T = 2 u_y / g", "highlight": true, "position": {"x": 520, "y": 150}}]},
			  {"stepNumber": 3, "title": "Range", "narration": "The range is the horizontal speed times the time.",
			   "canvas": {"actions": [
			    {"type": "DRAW_POINT", "animationDuration": 300,
			     "config": {"position": {"x": 275, "y": 100}, "label": "peak", "color": "#E74C3C", "radius": 5}}
			   ]},
			   "equations": [{"latex": "R = u_x * T", "highlight": false, "position": {"x": 520, "y": 200}}]}
			 ],
			 "summary": {"narration": "So the range is twenty root three metres.",
			   "keyResults": [{"label": "Range", "value": "20 sqrt(3) m"}]},
			 "masteryCheck": {"question": "What happens to the range at 30 degrees?",
			   "options": [
			    {"id": "A", "text": "It increases", "correct": false},
			    {"id": "B", "text": "It stays the same", "correct": true},
			    {"id": "C", "text": "It decreases", "correct": false},
			    {"id": "D", "text": "It cannot be found", "correct": false}],
			   "explanation": "Complementary angles give the same range."}
			}
			""";

	private ValidLessons() {
	}

	public static String validJson() {
		return VALID_JSON;
	}

	public static LessonResponse valid() {
		return lesson(VALID_JSON);
	}

	public static LessonResponse lesson(String json) {
		return JSON.readValue(json, LessonResponse.class);
	}

	/** The valid lesson with one piece of its JSON replaced, so a test breaks exactly one rule. */
	public static LessonResponse lessonWith(String original, String replacement) {
		return lesson(VALID_JSON.replace(original, replacement));
	}

	/** The lesson with other steps; totalSteps follows the new count. */
	public static LessonResponse withSteps(LessonResponse base, List<Step> steps) {
		return withParts(base, steps, base.summary(), base.masteryCheck());
	}

	public static LessonResponse withParts(LessonResponse base, List<Step> steps, Summary summary,
			MasteryCheck mastery) {
		return new LessonResponse(base.lessonId(), base.title(), base.subject(), base.topic(), base.difficulty(),
				steps.size(), base.estimatedDurationSeconds(), steps, summary, mastery);
	}
}
