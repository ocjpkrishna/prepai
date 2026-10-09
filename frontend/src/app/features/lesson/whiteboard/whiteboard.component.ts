import { ChangeDetectionStrategy, Component, computed, ElementRef, effect, inject, input, viewChild } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { clamp01, cursorAt, fadeOut, progress } from './playback';
import { HighlightItem, Scene, SceneItem, ShapeItem, StrokeDraw, TextItem } from './scene.model';

const LABEL_FADE_START = 0.7;
const SCROLL_BAND_TOP = 0.25;
const SCROLL_BAND_BOTTOM = 0.7;

@Component({
	selector: 'app-whiteboard',
	changeDetection: ChangeDetectionStrategy.OnPush,
	templateUrl: './whiteboard.component.html',
	styleUrl: './whiteboard.component.scss',
})
export class WhiteboardComponent {
	readonly scene = input.required<Scene>();
	readonly time = input.required<number>();
	readonly theme = input<'whiteboard' | 'chalkboard'>('whiteboard');
	readonly follow = input(true);

	private readonly sanitizer = inject(DomSanitizer);
	private readonly scroller = viewChild.required<ElementRef<HTMLElement>>('scroller');
	private readonly safe = new Map<string, SafeHtml>();

	protected readonly highlights = computed(() => this.scene().items.filter((i): i is HighlightItem => i.type === 'highlight' && i.w > 0));
	protected readonly content = computed(() => this.scene().items.filter((i) => i.type !== 'highlight'));
	protected readonly cursor = computed(() => cursorAt(this.scene(), this.time()));

	constructor() {
		effect(() => this.followCursor());
	}

	protected visible(item: SceneItem): boolean {
		return this.time() >= item.start && fadeOut(item, this.time()) > 0;
	}

	protected opacity(item: SceneItem): number {
		return fadeOut(item, this.time());
	}

	protected p(item: SceneItem): number {
		return progress(item, this.time());
	}

	protected strokeP(item: ShapeItem, s: StrokeDraw): number {
		const span = s.to - s.from || 1;
		return clamp01((this.p(item) - s.from) / span);
	}

	protected labelOpacity(item: ShapeItem): number {
		return clamp01((this.p(item) - LABEL_FADE_START) / (1 - LABEL_FADE_START));
	}

	protected asShape(item: SceneItem): ShapeItem | null {
		return item.type === 'shape' ? item : null;
	}

	protected asText(item: SceneItem): TextItem | null {
		return item.type === 'text' || item.type === 'math' ? item : null;
	}

	protected html(item: TextItem): SafeHtml {
		let cached = this.safe.get(item.id);
		if (!cached) {
			cached = this.sanitizer.bypassSecurityTrustHtml(item.html ?? '');
			this.safe.set(item.id, cached);
		}
		return cached;
	}

	protected col(name: string): string {
		return `var(--c-${name})`;
	}

	protected reveal(item: TextItem): string {
		return `inset(-4px ${(1 - this.p(item)) * 100}% -4px -4px)`;
	}

	private followCursor(): void {
		const c = this.cursor();
		const el = this.scroller().nativeElement;
		if (!c || !this.follow() || el.clientWidth === 0) {
			return;
		}
		const scale = el.clientWidth / this.scene().width;
		const y = c.y * scale;
		const top = el.scrollTop;
		const view = el.clientHeight;
		if (y < top + view * SCROLL_BAND_TOP || y > top + view * SCROLL_BAND_BOTTOM) {
			el.scrollTo({ top: Math.max(0, y - view * 0.4), behavior: 'smooth' });
		}
	}
}
