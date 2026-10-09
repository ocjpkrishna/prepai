import { Injectable, inject } from '@angular/core';
import { normalizeForSpeech } from './speech-normalizer';
import { TtsEngine } from './tts-engine';

interface Clip {
	url: string;
	duration: number;
}

@Injectable({ providedIn: 'root' })
export class NarrationService {
	private readonly engine = inject(TtsEngine);
	private clips: Clip[] = [];
	private element: HTMLAudioElement | null = null;

	get ready(): boolean {
		return this.clips.length > 0;
	}

	async prepare(narrations: string[], voice: string, onProgress: (done: number) => void): Promise<number[] | null> {
		this.release();
		const made: Clip[] = [];
		try {
			for (const text of narrations) {
				const url = URL.createObjectURL(await this.engine.synthesize(normalizeForSpeech(text), voice));
				made.push({ url, duration: await this.durationOf(url) });
				onProgress(made.length);
			}
		} catch {
			made.forEach((c) => URL.revokeObjectURL(c.url));
			return null;
		}
		this.clips = made;
		return made.map((c) => c.duration);
	}

	start(index: number, offset: number, rate: number, muted: boolean): void {
		const clip = this.clips[index];
		const audio = this.audio();
		audio.pause();
		if (!clip) {
			return;
		}
		audio.onloadedmetadata = () => {
			audio.currentTime = Math.min(Math.max(0, offset), clip.duration);
			void audio.play().catch(() => undefined);
		};
		audio.src = clip.url;
		audio.muted = muted;
		audio.playbackRate = rate;
		audio.load();
	}

	position(): number | null {
		const a = this.element;
		return a && !a.paused && !a.ended && a.currentTime > 0 ? a.currentTime : null;
	}

	pause(): void {
		this.element?.pause();
	}

	resume(): void {
		const a = this.element;
		if (a && a.src && !a.ended) {
			void a.play().catch(() => undefined);
		}
	}

	stop(): void {
		const a = this.element;
		if (a) {
			a.onloadedmetadata = null;
			a.pause();
		}
	}

	setRate(rate: number): void {
		if (this.element) {
			this.element.playbackRate = rate;
		}
	}

	setMuted(muted: boolean): void {
		if (this.element) {
			this.element.muted = muted;
		}
	}

	release(): void {
		this.stop();
		this.clips.forEach((c) => URL.revokeObjectURL(c.url));
		this.clips = [];
	}

	private audio(): HTMLAudioElement {
		this.element ??= new Audio();
		return this.element;
	}

	private durationOf(url: string): Promise<number> {
		return new Promise((resolve, reject) => {
			const probe = new Audio();
			probe.preload = 'metadata';
			probe.onloadedmetadata = () => resolve(probe.duration);
			probe.onerror = () => reject(new Error('unreadable audio'));
			probe.src = url;
		});
	}
}
