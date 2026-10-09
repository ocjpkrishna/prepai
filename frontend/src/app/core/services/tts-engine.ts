import { Injectable, InjectionToken, inject } from '@angular/core';

export const TTS_BASE_URL = new InjectionToken<string>('TTS_BASE_URL', {
	providedIn: 'root',
	factory: () => new URL('tts-api/', document.baseURI).toString(),
});

const PROBE_TIMEOUT_MS = 3000;
const SYNTHESIS_TIMEOUT_MS = 90000;

export abstract class TtsEngine {
	abstract voices(): Promise<string[] | null>;
	abstract synthesize(text: string, voice: string): Promise<Blob>;
}

@Injectable()
export class KokoroTtsEngine extends TtsEngine {
	private readonly base = inject(TTS_BASE_URL);

	async voices(): Promise<string[] | null> {
		try {
			const response = await fetch(`${this.base}voices`, { signal: AbortSignal.timeout(PROBE_TIMEOUT_MS) });
			const list: unknown = response.ok ? await response.json() : null;
			return Array.isArray(list) && list.length > 0 ? (list as string[]) : null;
		} catch {
			return null;
		}
	}

	async synthesize(text: string, voice: string): Promise<Blob> {
		const response = await fetch(`${this.base}tts`, {
			method: 'POST',
			headers: { 'Content-Type': 'application/json' },
			body: JSON.stringify({ text, voice, speed: 1, lang: voice.startsWith('b') ? 'en-gb' : 'en-us' }),
			signal: AbortSignal.timeout(SYNTHESIS_TIMEOUT_MS),
		});
		if (!response.ok) {
			throw new Error(`TTS service answered ${response.status}`);
		}
		return response.blob();
	}
}
