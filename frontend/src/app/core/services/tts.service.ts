import { HttpClient } from '@angular/common/http';
import { inject, Injectable, InjectionToken, signal } from '@angular/core';
import { firstValueFrom, Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { SynthesizeResult, TtsLanguage, Voice } from '../models/tts.model';
import { BrowserSpeech } from './browser-speech.service';

const TTS_PATH = `${environment.apiBaseUrl}/tts`;
const PREFERRED_LANGUAGE_PREFIX = 'en-IN';

/** Makes the one audio element the service reuses; tests swap it for a stand-in. */
export const AUDIO_FACTORY = new InjectionToken<() => HTMLAudioElement>('AUDIO_FACTORY', {
  providedIn: 'root',
  factory: () => () => new Audio(),
});

/** Prefers an Indian English voice, then any English one, then the first. */
export function pickPreferredVoice(voices: Voice[]): Voice | null {
  return (
    voices.find((voice) => voice.language.startsWith(PREFERRED_LANGUAGE_PREFIX)) ??
    voices.find((voice) => voice.language.startsWith('en')) ??
    voices[0] ??
    null
  );
}

/**
 * Narration through the backend TTS proxy (spec 7.2). Server audio is always normal speed;
 * speed is applied only through playbackRate. When the server voice fails, the browser's own voice
 * takes over. A failure of both never throws: speak() resolves false and `available` turns false so
 * the player can switch to silent mode with captions.
 */
@Injectable({ providedIn: 'root' })
export class TtsService {
  /** False after a TTS failure, until retry(). */
  readonly available = signal(true);

  private readonly http = inject(HttpClient);
  private readonly audio = inject(AUDIO_FACTORY)();
  private readonly speech = inject(BrowserSpeech);
  private readonly requests = new Map<string, Promise<SynthesizeResult>>();
  private speed = 1;
  private paused = false;
  private settle: ((ok: boolean) => void) | null = null;
  private runId = 0;
  private serverDown = false;
  private browserVoice = false;

  voices(): Observable<Voice[]> {
    return this.http.get<Voice[]>(`${TTS_PATH}/voices`);
  }

  /** Asks for the audio now so the next speak() of the same text starts without waiting. */
  prefetch(text: string, language: TtsLanguage): void {
    if (!this.available() || this.serverDown || text.trim() === '') {
      return;
    }
    this.request(text, language)
      .then((result) => this.warmCache(result.audioUrl))
      .catch(() => undefined);
  }

  /**
   * Plays the text and resolves when it ends. Resolves true when it played or was stopped by
   * stop(), false when TTS failed. `onProgress` gets 0 to 1 while playing.
   */
  async speak(text: string, language: TtsLanguage, onProgress: (fraction: number) => void): Promise<boolean> {
    const run = ++this.runId;
    this.finish(true);
    if (this.serverDown && this.speech.supported()) {
      return this.speakInBrowser(run, text, language, onProgress);
    }
    try {
      const result = await this.request(text, language);
      if (run !== this.runId) {
        return true;
      }
      return await this.play(result.audioUrl, onProgress);
    } catch {
      this.requests.delete(this.key(text, language));
      return this.fallBack(run, text, language, onProgress);
    }
  }

  setSpeed(speed: number): void {
    this.speed = speed;
    this.audio.playbackRate = speed;
  }

  pause(): void {
    this.paused = true;
    this.audio.pause();
    if (this.browserVoice) {
      this.speech.pause();
    }
  }

  resume(): void {
    this.paused = false;
    if (this.browserVoice) {
      this.speech.resume();
    } else if (this.settle) {
      this.startPlayback();
    }
  }

  /** Stops the current narration; its speak() resolves true. */
  stop(): void {
    this.runId += 1;
    this.paused = false;
    this.audio.pause();
    if (this.browserVoice) {
      this.browserVoice = false;
      this.speech.cancel();
    }
    this.finish(true);
  }

  /** Tries voice again after a failure. */
  retry(): void {
    this.serverDown = false;
    this.available.set(true);
  }

  /** The server voice failed: speak with the browser's voice when it has one, else go silent. */
  private fallBack(
    run: number,
    text: string,
    language: TtsLanguage,
    onProgress: (fraction: number) => void,
  ): Promise<boolean> | boolean {
    if (!this.speech.supported()) {
      return this.fail();
    }
    this.serverDown = true;
    return run === this.runId ? this.speakInBrowser(run, text, language, onProgress) : true;
  }

  private async speakInBrowser(
    run: number,
    text: string,
    language: TtsLanguage,
    onProgress: (fraction: number) => void,
  ): Promise<boolean> {
    this.browserVoice = true;
    const spoken = await this.speech.speak(text, language, this.speed, onProgress);
    if (run === this.runId) {
      this.browserVoice = false;
    }
    return spoken ? true : this.fail();
  }

  private request(text: string, language: TtsLanguage): Promise<SynthesizeResult> {
    const key = this.key(text, language);
    let pending = this.requests.get(key);
    if (!pending) {
      pending = firstValueFrom(this.http.post<SynthesizeResult>(`${TTS_PATH}/synthesize`, { text, language }));
      this.requests.set(key, pending);
    }
    return pending;
  }

  private play(url: string, onProgress: (fraction: number) => void): Promise<boolean> {
    return new Promise<boolean>((resolve) => {
      this.settle = resolve;
      this.audio.onended = () => this.finish(true);
      this.audio.onerror = () => this.fail();
      this.audio.ontimeupdate = () => onProgress(this.fractionPlayed());
      this.audio.src = url;
      this.audio.playbackRate = this.speed;
      if (!this.paused) {
        this.startPlayback();
      }
    });
  }

  private startPlayback(): void {
    this.audio.playbackRate = this.speed;
    Promise.resolve(this.audio.play()).catch(() => this.fail());
  }

  private fractionPlayed(): number {
    const { currentTime, duration } = this.audio;
    return duration > 0 && Number.isFinite(duration) ? Math.min(1, currentTime / duration) : 0;
  }

  private fail(): boolean {
    this.available.set(false);
    this.audio.pause();
    this.finish(false);
    return false;
  }

  private finish(ok: boolean): void {
    const settle = this.settle;
    this.settle = null;
    this.audio.onended = null;
    this.audio.onerror = null;
    this.audio.ontimeupdate = null;
    settle?.(ok);
  }

  private warmCache(url: string): void {
    if (typeof fetch === 'function') {
      fetch(url).catch(() => undefined);
    }
  }

  private key(text: string, language: TtsLanguage): string {
    return `${language}|${text}`;
  }
}
