package com.ascorp.prepai.generation.validation.rule;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction.ActionType;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Position;
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
 * The coordinate part of the canvas layer (spec 2.6): every {x, y} point in an action's config lies inside the
 * 800 x 500 canvas, drawing actions stay in x 0 to 500, and equations sit in the panel at x 500 to 800.
 */
@Component
public class CanvasBoundsRule implements ValidationRule {

	private static final int CANVAS_WIDTH = 800;
	private static final int CANVAS_HEIGHT = 500;
	private static final int DRAWING_MAX_X = 500;
	private static final int PANEL_MIN_X = 500;

	@Override
	public List<ValidationError> check(LessonResponse lesson) {
		return LessonParts.steps(lesson)
				.flatMap(step -> Stream.concat(actionErrors(step), equationErrors(step)))
				.toList();
	}

	private Stream<ValidationError> actionErrors(Step step) {
		return LessonParts.actions(step).stream()
				.filter(action -> action.type() != null)
				.flatMap(action -> pointErrors(step, action, action.config()).stream());
	}

	private List<ValidationError> pointErrors(Step step, CanvasAction action, Object node) {
		if (node instanceof Map<?, ?> map && map.get("x") instanceof Number x && map.get("y") instanceof Number y) {
			return outOfBounds(step, action, x.doubleValue(), y.doubleValue());
		}
		return childNodes(node).stream()
				.flatMap(child -> pointErrors(step, action, child).stream())
				.toList();
	}

	private List<?> childNodes(Object node) {
		return switch (node) {
			case Map<?, ?> map -> new ArrayList<>(map.values());
			case List<?> list -> list;
			case null, default -> List.of();
		};
	}

	private List<ValidationError> outOfBounds(Step step, CanvasAction action, double x, double y) {
		List<ValidationError> errors = new ArrayList<>();
		if (x < 0 || x > CANVAS_WIDTH || y < 0 || y > CANVAS_HEIGHT) {
			errors.add(new ValidationError(ValidationLayer.CANVAS, ValidationCode.COORDINATE_BOUNDS,
					"Step %d %s has point (%s, %s) outside the %d x %d canvas"
							.formatted(step.stepNumber(), action.type(), x, y, CANVAS_WIDTH, CANVAS_HEIGHT)));
		}
		if (isDrawing(action.type()) && x > DRAWING_MAX_X) {
			errors.add(new ValidationError(ValidationLayer.CANVAS, ValidationCode.DRAWING_BOUNDS,
					"Step %d %s has point x %s; drawings must stay at x 0 to %d"
							.formatted(step.stepNumber(), action.type(), x, DRAWING_MAX_X)));
		}
		return errors;
	}

	private boolean isDrawing(ActionType type) {
		return type.name().startsWith("DRAW_") || type == ActionType.HIGHLIGHT_REGION;
	}

	private Stream<ValidationError> equationErrors(Step step) {
		return LessonParts.equations(step).stream()
				.filter(equation -> !inPanel(equation.position()))
				.map(equation -> new ValidationError(ValidationLayer.CANVAS, ValidationCode.EQUATION_POSITION,
						"Step %d has an equation outside the panel (x %d to %d, y 0 to %d)"
								.formatted(step.stepNumber(), PANEL_MIN_X, CANVAS_WIDTH, CANVAS_HEIGHT)));
	}

	private boolean inPanel(Position position) {
		if (position == null) {
			return false;
		}
		return position.x() >= PANEL_MIN_X && position.x() <= CANVAS_WIDTH
				&& position.y() >= 0 && position.y() <= CANVAS_HEIGHT;
	}
}
