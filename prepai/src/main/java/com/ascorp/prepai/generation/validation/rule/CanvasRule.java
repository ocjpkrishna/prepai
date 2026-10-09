package com.ascorp.prepai.generation.validation.rule;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction.ActionType;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Step;
import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import com.ascorp.prepai.generation.validation.model.ValidationLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * The canvas layer of spec 2.6 for each action: the required config keys of spec 3.3, the animation duration and
 * the number of actions per step. Coordinates are checked by {@link CanvasBoundsRule}.
 */
@Component
public class CanvasRule implements ValidationRule {

	private static final int MAX_ACTIONS_PER_STEP = 40;
	private static final int MIN_ANIMATION_MS = 100;
	private static final int MAX_ANIMATION_MS = 5000;

	/** The config keys each action type must carry (spec 3.3). Axis labels are the xLabel and yLabel of spec 3.2. */
	private static final Map<ActionType, List<String>> REQUIRED_CONFIG = Map.ofEntries(
			Map.entry(ActionType.DRAW_AXIS, List.of("origin", "xLength", "yLength", "xLabel", "yLabel")),
			Map.entry(ActionType.DRAW_ARROW, List.of("from", "to", "label", "color", "angle")),
			Map.entry(ActionType.DRAW_DASHED_LINE, List.of("from", "to", "label", "color")),
			Map.entry(ActionType.DRAW_LINE, List.of("from", "to", "color", "strokeWidth")),
			Map.entry(ActionType.DRAW_ARC, List.of("center", "radius", "startAngle", "endAngle", "label")),
			Map.entry(ActionType.DRAW_PARABOLA, List.of("start", "peak", "end", "color")),
			Map.entry(ActionType.DRAW_CIRCLE, List.of("center", "radius", "color", "fill")),
			Map.entry(ActionType.DRAW_POINT, List.of("position", "label", "color", "radius")),
			Map.entry(ActionType.DRAW_DOUBLE_ARROW, List.of("from", "to", "label", "color")),
			Map.entry(ActionType.WRITE_TEXT, List.of("position", "text", "fontSize", "color")),
			Map.entry(ActionType.WRITE_LATEX, List.of("position", "latex", "fontSize")),
			Map.entry(ActionType.DRAW_VECTOR, List.of("origin", "magnitude", "angle", "label", "color")),
			Map.entry(ActionType.DRAW_FREE_BODY, List.of("center", "forces")),
			Map.entry(ActionType.DRAW_CIRCUIT, List.of("components")),
			Map.entry(ActionType.DRAW_GRAPH, List.of("fn", "xRange", "yRange", "color")),
			Map.entry(ActionType.HIGHLIGHT_REGION, List.of("points", "color", "opacity")),
			Map.entry(ActionType.CLEAR_CANVAS, List.of("keepElements")),
			Map.entry(ActionType.FADE_OUT, List.of("elementIds", "duration")));

	@Override
	public List<ValidationError> check(LessonResponse lesson) {
		return LessonParts.steps(lesson)
				.flatMap(step -> stepErrors(step).stream())
				.toList();
	}

	private List<ValidationError> stepErrors(Step step) {
		List<CanvasAction> actions = LessonParts.actions(step);
		List<ValidationError> errors = new ArrayList<>(actionCountErrors(step, actions));
		actions.forEach(action -> errors.addAll(actionErrors(step, action)));
		return errors;
	}

	private List<ValidationError> actionCountErrors(Step step, List<CanvasAction> actions) {
		if (actions.size() <= MAX_ACTIONS_PER_STEP) {
			return List.of();
		}
		return List.of(new ValidationError(ValidationLayer.CANVAS, ValidationCode.ACTION_COUNT,
				"Step %d has %d canvas actions; the limit is %d".formatted(step.stepNumber(), actions.size(),
						MAX_ACTIONS_PER_STEP)));
	}

	private List<ValidationError> actionErrors(Step step, CanvasAction action) {
		if (action.type() == null) {
			return List.of(new ValidationError(ValidationLayer.CANVAS, ValidationCode.ACTION_TYPE,
					"Step %d has a canvas action with no type".formatted(step.stepNumber())));
		}
		return Stream.of(missingConfigErrors(step, action), durationErrors(step, action))
				.flatMap(List::stream)
				.toList();
	}

	private List<ValidationError> missingConfigErrors(Step step, CanvasAction action) {
		List<String> missing = REQUIRED_CONFIG.getOrDefault(action.type(), List.of()).stream()
				.filter(key -> !hasValue(action.config(), key))
				.toList();
		if (missing.isEmpty()) {
			return List.of();
		}
		return List.of(new ValidationError(ValidationLayer.CANVAS, ValidationCode.ACTION_CONFIG,
				"Step %d %s is missing config: %s"
						.formatted(step.stepNumber(), action.type(), String.join(", ", missing))));
	}

	private boolean hasValue(Map<String, Object> config, String key) {
		return config != null && config.get(key) != null;
	}

	private List<ValidationError> durationErrors(Step step, CanvasAction action) {
		if (action.animationDuration() >= MIN_ANIMATION_MS && action.animationDuration() <= MAX_ANIMATION_MS) {
			return List.of();
		}
		return List.of(new ValidationError(ValidationLayer.CANVAS, ValidationCode.ANIMATION_DURATION,
				"Step %d %s has animationDuration %d; it must be %d to %d ms"
						.formatted(step.stepNumber(), action.type(), action.animationDuration(),
								MIN_ANIMATION_MS, MAX_ANIMATION_MS)));
	}
}
