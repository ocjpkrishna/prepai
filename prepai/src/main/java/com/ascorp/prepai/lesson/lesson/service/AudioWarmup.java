package com.ascorp.prepai.lesson.lesson.service;

import com.ascorp.prepai.common.model.lesson.LessonResponse;

/**
 * Prepares narration audio for a stored lesson in the background (spec 4.5). `speech` (row 16) supplies the real
 * implementation; until then {@link NoOpAudioWarmup} is used. An implementation must never throw or block:
 * a text-to-speech failure may not fail a lesson.
 */
public interface AudioWarmup {

	void warmUp(LessonResponse lesson);
}
