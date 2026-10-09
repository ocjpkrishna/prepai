package com.ascorp.prepai.generation.llm.service;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import com.ascorp.prepai.generation.quality.model.QualityProperties;
import com.ascorp.prepai.generation.quality.model.VerifierGrade;
import com.ascorp.prepai.generation.quality.service.VerifierClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientResponseException;

/**
 * Claude Fable 5.1 as the nightly verifier (spec 8.1). It lives with the other Claude calls, so every Claude failure is
 * translated by {@link ClaudeFailures}. Only the production profile selects it; no test or build step calls it.
 */
@Component
@Profile("prod")
public class ClaudeVerifierClient implements VerifierClient {

	private final ChatModel chatModel;
	private final QualityProperties properties;

	public ClaudeVerifierClient(ChatModel chatModel, QualityProperties properties) {
		this.chatModel = chatModel;
		this.properties = properties;
	}

	@Override
	public VerifierGrade grade(String problemText, LessonResponse lesson) {
		return VerifierExchange.grade(call(problemText, lesson));
	}

	private ChatResponse call(String problemText, LessonResponse lesson) {
		try {
			return chatModel.call(VerifierExchange.prompt(properties.verifierModel(), problemText, lesson));
		} catch (RestClientResponseException e) {
			throw ClaudeFailures.status(e);
		} catch (ResourceAccessException e) {
			throw ClaudeFailures.io(e);
		}
	}
}
