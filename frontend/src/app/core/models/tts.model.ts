export type TtsLanguage = 'en-IN' | 'hi-IN';

/** Answer of POST /tts/synthesize (spec 4.5). */
export interface SynthesizeResult {
  audioUrl: string;
  durationMs: number;
}

/** One entry of GET /tts/voices. */
export interface Voice {
  id: string;
  name: string;
  language: string;
}

export const PLAYBACK_SPEEDS = [0.5, 0.75, 1, 1.25, 1.5, 2] as const;
