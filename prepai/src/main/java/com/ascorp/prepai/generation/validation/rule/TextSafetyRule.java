package com.ascorp.prepai.generation.validation.rule;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.common.model.lesson.LessonResponse.CanvasAction.ActionType;
import com.ascorp.prepai.common.model.lesson.LessonResponse.Step;
import com.ascorp.prepai.generation.validation.model.ValidationCode;
import com.ascorp.prepai.generation.validation.model.ValidationError;
import com.ascorp.prepai.generation.validation.model.ValidationLayer;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.stereotype.Component;

/**
 * The text safety layer of spec 2.6, after the sanitiser has stripped HTML: narration must be speakable (no LaTeX or
 * markup characters, at most 600 characters per step), and no LaTeX string may use a denied command.
 */
@Component
public class TextSafetyRule implements ValidationRule {

	private static final int MAX_NARRATION_CHARS = 600;
	private static final String MARKUP_CHARS = "\\_^${}";
	private static final List<String> LATEX_DENYLIST =
			List.of("\\input", "\\include", "\\href", "\\url", "\\write", "\\def", "\\csname");
	private static final String LATEX_KEY = "latex";

	@Override
	public List<ValidationError> check(LessonResponse lesson) {
		return Stream.of(stepNarrationErrors(lesson), summaryNarrationErrors(lesson), latexErrors(lesson))
				.flatMap(List::stream)
				.toList();
	}

	private List<ValidationError> stepNarrationErrors(LessonResponse lesson) {
		return LessonParts.steps(lesson)
				.flatMap(step -> stepNarrationErrors(step).stream())
				.toList();
	}

	private List<ValidationError> stepNarrationErrors(Step step) {
		String narration = Objects.toString(step.narration(), "");
		List<ValidationError> errors = new ArrayList<>(markupErrors("Step " + step.stepNumber(), narration));
		if (narration.length() > MAX_NARRATION_CHARS) {
			errors.add(new ValidationError(ValidationLayer.TEXT_SAFETY, ValidationCode.NARRATION_LENGTH,
					"Step %d narration has %d characters; the limit is %d"
							.formatted(step.stepNumber(), narration.length(), MAX_NARRATION_CHARS)));
		}
		return errors;
	}

	private List<ValidationError> summaryNarrationErrors(LessonResponse lesson) {
		if (lesson.summary() == null) {
			return List.of();
		}
		return markupErrors("The summary", Objects.toString(lesson.summary().narration(), ""));
	}

	private List<ValidationError> markupErrors(String where, String narration) {
		boolean hasMarkup = narration.chars().anyMatch(character -> MARKUP_CHARS.indexOf(character) >= 0);
		if (!hasMarkup) {
			return List.of();
		}
		return List.of(new ValidationError(ValidationLayer.TEXT_SAFETY, ValidationCode.NARRATION_MARKUP,
				"%s narration contains LaTeX or markup characters (\\ _ ^ $ { }); write it as speech"
						.formatted(where)));
	}

	private List<ValidationError> latexErrors(LessonResponse lesson) {
		return latexStrings(lesson)
				.filter(this::hasDeniedCommand)
				.map(latex -> new ValidationError(ValidationLayer.TEXT_SAFETY, ValidationCode.LATEX_DENYLIST,
						"A LaTeX string uses a command that is not allowed (\\input, \\include, \\href, \\url, "
								+ "\\write, \\def, \\csname)"))
				.toList();
	}

	private Stream<String> latexStrings(LessonResponse lesson) {
		return LessonParts.steps(lesson)
				.flatMap(step -> Stream.concat(equationLatex(step), writtenLatex(step)));
	}

	private Stream<String> equationLatex(Step step) {
		return LessonParts.equations(step).stream()
				.map(equation -> equation.latex())
				.filter(Objects::nonNull);
	}

	private Stream<String> writtenLatex(Step step) {
		return LessonParts.actions(step).stream()
				.filter(action -> action.type() == ActionType.WRITE_LATEX && action.config() != null)
				.map(action -> action.config().get(LATEX_KEY))
				.filter(String.class::isInstance)
				.map(String.class::cast);
	}

	private boolean hasDeniedCommand(String latex) {
		return LATEX_DENYLIST.stream().anyMatch(latex::contains);
	}
}
