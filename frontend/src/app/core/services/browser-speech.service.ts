import { Injectable } from '@angular/core';
import { TtsLanguage } from '../models/tts.model';

const FEMALE_NAMES =
  /female|woman|zira|heera|neerja|veena|kalpana|lekha|swara|priya|samantha|karen|moira|tessa|fiona|victoria|susan|hazel|linda|catherine|sonia|libby|aria|jenny|emma|ava|serena|allison|kate|amy|joanna|salli|kendra|kimberly|ivy/i;
const MALE_NAMES =
  /(^|[^a-z])male(?!.*female)|david|mark|james|george|ravi|hemant|prabhat|alex|daniel|fred|rishi|aaron|thomas|guy|liam|brian|matthew|joey|justin|ryan|arthur|oliver/i;
const NATURAL_NAMES = /natural|neural|online|enhanced|premium/i;
const SENTENCE_END = /(?<=[.!?\u0964])\s+/;
const VOICES_WAIT_MS = 1000;
const FALLBACK_PITCH = 1.2;

/** Picks the most natural female voice for the language, or null when the device has none. */
export function pickFemaleVoice(voices: readonly SpeechSynthesisVoice[], language: TtsLanguage): SpeechSynthesisVoice | null {
  const scored = voices
    .filter((voice) => isFemale(voice))
    .map((voice) => ({ voice, score: score(voice, language) }))
    .filter((candidate) => candidate.score > 0)
    .sort((a, b) => b.score - a.score);
  return scored[0]?.voice ?? null;
}

function isFemale(voice: SpeechSynthesisVoice): boolean {
  return FEMALE_NAMES.test(voice.name) || (!MALE_NAMES.test(voice.name) && /female/i.test(voice.name));
}

function score(voice: SpeechSynthesisVoice, language: TtsLanguage): number {
  const languageScore = voice.lang === language ? 50 : voice.lang.startsWith(language.slice(0, 2)) ? 20 : 0;
  if (languageScore === 0) {
    return 0;
  }
  return languageScore + (NATURAL_NAMES.test(voice.name) ? 30 : 0) + (/google/i.test(voice.name) ? 10 : 0);
}

/**
 * The browser's own voice, used when the server voice is not available (spec 7.2 lets the player stay
 * silent; this keeps it speaking). It needs no server, key or download. It always uses a female voice and
 * speaks one sentence at a time, which sounds less flat than one long utterance.
 */
@Injectable({ providedIn: 'root' })
export class BrowserSpeech {
  supported(): boolean {
    return typeof window !== 'undefined' && 'speechSynthesis' in window && typeof SpeechSynthesisUtterance === 'function';
  }

  /**
   * Speaks the text and resolves true when it ends or is cancelled, false when the browser fails.
   * `onProgress` gets 0 to 1 as the voice moves through the text.
   */
  async speak(text: string, language: TtsLanguage, rate: number, onProgress: (fraction: number) => void): Promise<boolean> {
    const voice = pickFemaleVoice(await this.voices(), language);
    return new Promise<boolean>((resolve) => {
      const sentences = text.split(SENTENCE_END).filter((sentence) => sentence.trim() !== '');
      const total = Math.max(1, text.length);
      let spoken = 0;
      window.speechSynthesis.cancel();
      sentences.forEach((sentence, index) => {
        const utterance = new SpeechSynthesisUtterance(sentence);
        utterance.lang = language;
        utterance.rate = rate;
        utterance.pitch = voice ? 1 : FALLBACK_PITCH;
        utterance.voice = voice;
        utterance.onend = () => {
          spoken += sentence.length + 1;
          onProgress(Math.min(1, spoken / total));
          if (index === sentences.length - 1) {
            resolve(true);
          }
        };
        utterance.onerror = (event) => resolve(event.error === 'canceled' || event.error === 'interrupted');
        window.speechSynthesis.speak(utterance);
      });
      if (sentences.length === 0) {
        resolve(true);
      }
    });
  }

  pause(): void {
    window.speechSynthesis.pause();
  }

  resume(): void {
    window.speechSynthesis.resume();
  }

  cancel(): void {
    window.speechSynthesis.cancel();
  }

  /** Chrome loads its voices late: wait for them once, but never longer than a second. */
  private voices(): Promise<SpeechSynthesisVoice[]> {
    const now = window.speechSynthesis.getVoices();
    if (now.length > 0) {
      return Promise.resolve(now);
    }
    return new Promise((resolve) => {
      const done = (): void => resolve(window.speechSynthesis.getVoices());
      window.speechSynthesis.addEventListener?.('voiceschanged', done, { once: true });
      setTimeout(done, VOICES_WAIT_MS);
    });
  }
}
