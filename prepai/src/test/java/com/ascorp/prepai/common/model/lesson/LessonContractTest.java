package com.ascorp.prepai.common.model.lesson;

import static org.assertj.core.api.Assertions.assertThat;

import com.ascorp.prepai.common.model.enums.Exam;
import com.ascorp.prepai.common.model.enums.Language;
import com.ascorp.prepai.common.model.enums.Subject;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** Checks that the JSON of spec 3.1 and 3.2 maps onto the contract records, field for field. */
class LessonContractTest {

	private static final String REQUEST_JSON = """
			{"type":"TOPIC","subject":"PHYSICS","exam":"JEE_MAIN",
			 "input":{"text":"Projectile motion","imageBase64":null},
			 "difficulty":"MEDIUM","language":"en"}
			""";

	private static final String RESPONSE_JSON = """
			{"lessonId":"3f0c6a52-6b1e-4d0c-9a57-2f8e1c1b7d10","title":"Projectile Motion","subject":"PHYSICS",
			 "topic":"Kinematics","difficulty":"MEDIUM","totalSteps":1,"estimatedDurationSeconds":60,
			 "steps":[{"stepNumber":1,"title":"Resolve velocity","narration":"Break it up.",
			   "canvas":{"actions":[{"type":"DRAW_AXIS","config":{"xLength":400},"animationDuration":1000}]},
			   "equations":[{"latex":"u_x = 10","highlight":true,"position":{"x":520,"y":150}}]}],
			 "summary":{"narration":"Done.","keyResults":[{"label":"Range","value":"34.6 m"}]},
			 "masteryCheck":{"question":"Which is right?","options":[{"id":"A","text":"Yes","correct":true}],
			   "explanation":"Because."}}
			""";

	private final JsonMapper jsonMapper = JsonMapper.builder().build();

	@Test
	void readsALessonRequestWithLowercaseLanguageCode() {
		LessonRequest request = jsonMapper.readValue(REQUEST_JSON, LessonRequest.class);

		assertThat(request.type()).isEqualTo(LessonRequest.Type.TOPIC);
		assertThat(request.subject()).isEqualTo(Subject.PHYSICS);
		assertThat(request.exam()).isEqualTo(Exam.JEE_MAIN);
		assertThat(request.language()).isEqualTo(Language.EN);
		assertThat(request.input().text()).isEqualTo("Projectile motion");
	}

	@Test
	void readsALessonResponseWithStepsCanvasAndMasteryCheck() {
		LessonResponse response = jsonMapper.readValue(RESPONSE_JSON, LessonResponse.class);

		LessonResponse.Step step = response.steps().getFirst();
		assertThat(response.title()).isEqualTo("Projectile Motion");
		assertThat(step.canvas().actions().getFirst().type())
				.isEqualTo(LessonResponse.CanvasAction.ActionType.DRAW_AXIS);
		assertThat(step.equations().getFirst().position()).isEqualTo(new LessonResponse.Position(520, 150));
		assertThat(response.masteryCheck().options().getFirst().correct()).isTrue();
	}
}
