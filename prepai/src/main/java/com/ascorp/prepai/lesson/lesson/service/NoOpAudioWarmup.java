package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.model.lesson.LessonResponse;
import org.springframework.stereotype.Component;

/** Default until `speech` exists: the player asks for audio itself, so doing nothing here is safe. */
@Component
public class NoOpAudioWarmup implements AudioWarmup {

	@Override
	public void warmUp(LessonResponse lesson) {
		// nothing to prepare
	}
}
