import { Injectable } from '@angular/core';

const BASE_RATE = 0.98;

@Injectable({ providedIn: 'root' })
export class SpeechService {
	private get synth(): SpeechSynthesis | null {
		return typeof speechSynthesis === 'undefined' ? null : speechSynthesis;
	}

	speak(text: string, rate: number): void {
		const synth = this.synth;
		if (!synth) {
			return;
		}
		synth.cancel();
		const utterance = new SpeechSynthesisUtterance(text);
		utterance.lang = 'en-IN';
		utterance.rate = BASE_RATE * rate;
		synth.speak(utterance);
	}

	pause(): void {
		this.synth?.pause();
	}

	resume(): void {
		this.synth?.resume();
	}

	stop(): void {
		this.synth?.cancel();
	}
}
