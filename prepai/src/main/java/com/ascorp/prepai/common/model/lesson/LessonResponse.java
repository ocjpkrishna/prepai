package com.ascorp.prepai.common.model.lesson;

import com.ascorp.prepai.common.model.enums.Difficulty;
import com.ascorp.prepai.common.model.enums.Subject;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** The validated lesson the LLM must produce and the frontend draws (spec 3.2). */
public record LessonResponse(
		UUID lessonId,
		String title,
		Subject subject,
		String topic,
		Difficulty difficulty,
		int totalSteps,
		int estimatedDurationSeconds,
		List<Step> steps,
		Summary summary,
		MasteryCheck masteryCheck) {

	/** The same lesson under another id: each student who is served a cached lesson gets a lesson of their own. */
	public LessonResponse withLessonId(UUID id) {
		return new LessonResponse(id, title, subject, topic, difficulty, totalSteps, estimatedDurationSeconds, steps,
				summary, masteryCheck);
	}

	/** One step of the whiteboard walkthrough: what is drawn, what is written and what is said. */
	public record Step(int stepNumber, String title, String narration, Canvas canvas, List<Equation> equations) {
	}

	public record Canvas(List<CanvasAction> actions) {
	}

	/** One drawing command. The config keys depend on the type (spec 3.3), so they stay a map. */
	public record CanvasAction(ActionType type, Map<String, Object> config, int animationDuration) {

		public enum ActionType {
			DRAW_AXIS,
			DRAW_ARROW,
			DRAW_DASHED_LINE,
			DRAW_LINE,
			DRAW_ARC,
			DRAW_PARABOLA,
			DRAW_CIRCLE,
			DRAW_POINT,
			DRAW_DOUBLE_ARROW,
			WRITE_TEXT,
			WRITE_LATEX,
			DRAW_VECTOR,
			DRAW_FREE_BODY,
			DRAW_CIRCUIT,
			DRAW_GRAPH,
			HIGHLIGHT_REGION,
			CLEAR_CANVAS,
			FADE_OUT
		}
	}

	public record Equation(String latex, boolean highlight, Position position) {
	}

	/** Canvas coordinates in logical pixels (spec 3.4). */
	public record Position(int x, int y) {
	}

	public record Summary(String narration, List<KeyResult> keyResults) {
	}

	public record KeyResult(String label, String value) {
	}

	public record MasteryCheck(String question, List<Option> options, String explanation) {
	}

	public record Option(String id, String text, boolean correct) {
	}
}
