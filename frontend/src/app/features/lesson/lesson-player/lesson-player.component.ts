import { ChangeDetectionStrategy, Component, computed, DestroyRef, effect, inject, input, signal, untracked } from '@angular/core';
import { RouterLink } from '@angular/router';
import { Lesson } from '../../../core/models/whiteboard.model';
import { LessonSource } from '../../../core/services/lesson-source';
import { NarrationService } from '../../../core/services/narration.service';
import { SpeechService } from '../../../core/services/speech.service';
import { TtsEngine } from '../../../core/services/tts-engine';
import { buildScene } from '../whiteboard/board-layout';
import { clamp01, stepIndexAt, wordIndexAt } from '../whiteboard/playback';
import { Scene } from '../whiteboard/scene.model';
import { domMeasurer, estimateMeasurer } from '../whiteboard/text-measure';
import { WhiteboardComponent } from '../whiteboard/whiteboard.component';

const SPEEDS = [0.75, 1, 1.25, 1.5, 2];
const FONT_WAIT_MS = 1500;
const VOICE_KEY = 'prepai.voice';
const DEFAULT_VOICE = 'af_heart';

@Component({
	selector: 'app-lesson-player',
	changeDetection: ChangeDetectionStrategy.OnPush,
	imports: [RouterLink, WhiteboardComponent],
	templateUrl: './lesson-player.component.html',
	styleUrl: './lesson-player.component.scss',
})
export class LessonPlayerComponent {
	readonly id = input.required<string>();

	private readonly source = inject(LessonSource);
	private readonly speech = inject(SpeechService);
	private readonly narration = inject(NarrationService);
	private readonly engine = inject(TtsEngine);

	protected readonly speeds = SPEEDS;
	protected readonly lesson = signal<Lesson | null>(null);
	protected readonly scene = signal<Scene | null>(null);
	protected readonly missing = signal(false);
	protected readonly time = signal(0);
	protected readonly playing = signal(false);
	protected readonly started = signal(false);
	protected readonly speed = signal(1);
	protected readonly sound = signal(true);
	protected readonly chalk = signal(false);
	protected readonly picked = signal<string | null>(null);
	protected readonly voices = signal<string[] | null>(null);
	protected readonly voice = signal(this.savedVoice());
	protected readonly useClips = signal(false);
	protected readonly preparing = signal(false);
	protected readonly prepDone = signal(0);

	protected readonly stepIndex = computed(() => {
		const s = this.scene();
		return s ? stepIndexAt(s, this.time()) : 0;
	});
	protected readonly step = computed(() => this.lesson()?.steps[this.stepIndex()] ?? null);
	protected readonly words = computed(() => this.scene()?.steps[this.stepIndex()]?.words ?? []);
	protected readonly wordIndex = computed(() => {
		const s = this.scene();
		return s && this.started() ? wordIndexAt(s, this.time()) : -1;
	});
	protected readonly finished = computed(() => {
		const s = this.scene();
		return !!s && this.time() >= s.duration - 0.001;
	});
	protected readonly progress = computed(() => {
		const s = this.scene();
		return s ? clamp01(this.time() / s.duration) : 0;
	});
	protected readonly chosen = computed(() => this.lesson()?.masteryCheck?.options.find((o) => o.id === this.picked()) ?? null);
	protected readonly voiceLabel = computed(() => (this.useClips() ? `Voice: ${this.voice()}` : 'Voice: browser'));

	private raf = 0;
	private last = 0;
	private spokenStep = -1;
	private audioStep = -1;
	private loadToken = 0;

	constructor() {
		effect(() => {
			const id = this.id();
			untracked(() => void this.load(id));
		});
		effect(() => this.speakFallback());
		inject(DestroyRef).onDestroy(() => {
			cancelAnimationFrame(this.raf);
			this.speech.stop();
			this.narration.release();
		});
	}

	private savedVoice(): string {
		try {
			return localStorage.getItem(VOICE_KEY) ?? DEFAULT_VOICE;
		} catch {
			return DEFAULT_VOICE;
		}
	}

	private async load(id: string): Promise<void> {
		const token = ++this.loadToken;
		this.reset();
		const lesson = await this.source.get(id);
		if (token !== this.loadToken) {
			return;
		}
		if (!lesson) {
			this.missing.set(true);
			return;
		}
		await this.fontsReady();
		this.lesson.set(lesson);
		await this.prepare(lesson, token);
	}

	private async prepare(lesson: Lesson, token: number): Promise<void> {
		const measure = typeof document === 'undefined' ? estimateMeasurer() : domMeasurer(document);
		this.preparing.set(true);
		this.prepDone.set(0);
		const voices = await this.engine.voices();
		this.voices.set(voices);
		const voice = voices?.includes(this.voice()) ? this.voice() : (voices?.[0] ?? this.voice());
		this.voice.set(voice);
		const durations = voices
			? await this.narration.prepare(lesson.steps.map((s) => s.narration), voice, (done) => this.prepDone.set(done))
			: null;
		if (token !== this.loadToken) {
			return;
		}
		this.useClips.set(durations !== null);
		this.scene.set(buildScene(lesson, measure, durations ?? undefined));
		this.preparing.set(false);
	}

	private async fontsReady(): Promise<void> {
		if (typeof document === 'undefined' || !document.fonts) {
			return;
		}
		const faces = ['24px Kalam', '700 28px Kalam', '24px KaTeX_Main', 'italic 24px KaTeX_Math', '24px KaTeX_AMS', '24px KaTeX_Size1', '24px KaTeX_Size2', '700 24px KaTeX_Main'];
		const loaded = Promise.all([...faces.map((f) => document.fonts.load(f)), document.fonts.ready]);
		await Promise.race([loaded, new Promise((resolve) => setTimeout(resolve, FONT_WAIT_MS))]);
	}

	private reset(): void {
		cancelAnimationFrame(this.raf);
		this.speech.stop();
		this.narration.release();
		this.lesson.set(null);
		this.scene.set(null);
		this.missing.set(false);
		this.time.set(0);
		this.playing.set(false);
		this.started.set(false);
		this.useClips.set(false);
		this.spokenStep = -1;
		this.audioStep = -1;
		this.picked.set(null);
	}

	private speakFallback(): void {
		const step = this.step();
		const index = this.stepIndex();
		if (this.useClips() || !this.playing() || !this.sound() || !step || index === this.spokenStep) {
			return;
		}
		this.spokenStep = index;
		this.speech.speak(step.narration, this.speed());
	}

	protected start(): void {
		this.started.set(true);
		this.play();
	}

	protected play(): void {
		if (this.finished()) {
			this.time.set(0);
			this.spokenStep = -1;
			this.audioStep = -1;
		}
		this.playing.set(true);
		this.last = performance.now();
		if (this.useClips()) {
			this.narration.resume();
		} else {
			this.speech.resume();
		}
		this.raf = requestAnimationFrame((now) => this.tick(now));
	}

	protected pause(): void {
		this.playing.set(false);
		cancelAnimationFrame(this.raf);
		this.narration.pause();
		this.speech.pause();
	}

	protected toggle(): void {
		this.playing() ? this.pause() : this.started() ? this.play() : this.start();
	}

	private tick(now: number): void {
		const scene = this.scene();
		if (!this.playing() || !scene) {
			return;
		}
		const dt = ((now - this.last) / 1000) * this.speed();
		this.last = now;
		const t = Math.min(scene.duration, this.followAudio(scene, this.time() + dt));
		this.time.set(t);
		if (t >= scene.duration) {
			this.playing.set(false);
			this.narration.stop();
			return;
		}
		this.raf = requestAnimationFrame((n) => this.tick(n));
	}

	private followAudio(scene: Scene, t: number): number {
		if (!this.useClips()) {
			return t;
		}
		const index = stepIndexAt(scene, t);
		if (index !== this.audioStep) {
			this.audioStep = index;
			this.narration.start(index, t - scene.steps[index].start, this.speed(), !this.sound());
			return t;
		}
		const position = this.narration.position();
		return position === null ? t : scene.steps[index].start + position;
	}

	protected seekStep(index: number): void {
		const scene = this.scene();
		const target = scene?.steps[Math.max(0, Math.min(index, (scene?.steps.length ?? 1) - 1))];
		if (!scene || !target) {
			return;
		}
		this.spokenStep = -1;
		this.audioStep = -1;
		this.speech.stop();
		this.narration.stop();
		this.time.set(target.start + 0.01);
		this.started.set(true);
		if (!this.playing()) {
			this.play();
		}
	}

	protected setSpeed(value: number): void {
		this.speed.set(value);
		this.narration.setRate(value);
		this.spokenStep = -1;
	}

	protected toggleSound(): void {
		this.sound.update((v) => !v);
		this.narration.setMuted(!this.sound());
		if (!this.useClips()) {
			this.speech.stop();
			this.spokenStep = -1;
		}
	}

	protected async changeVoice(voice: string): Promise<void> {
		this.voice.set(voice);
		try {
			localStorage.setItem(VOICE_KEY, voice);
		} catch {
			// the choice just is not remembered
		}
		const lesson = this.lesson();
		if (!lesson) {
			return;
		}
		const token = ++this.loadToken;
		this.reset();
		this.lesson.set(lesson);
		await this.prepare(lesson, token);
	}

	protected pick(id: string): void {
		this.picked.set(id);
	}
}
