import { NgTemplateOutlet } from '@angular/common';
import { ChangeDetectionStrategy, Component, computed, ElementRef, effect, inject, input, viewChild, viewChildren } from '@angular/core';
import { DomSanitizer, SafeHtml } from '@angular/platform-browser';
import { clamp01, cursorAt, fadeOut, progress } from './playback';
import { HighlightItem, Layer, Scene, SceneItem, SectionInfo, ShapeItem, StrokeDraw, TextItem } from './scene.model';

interface Layered {
	highlights: HighlightItem[];
	content: SceneItem[];
}

interface Row {
	sec: SectionInfo;
	fig: Layered | null;
	col: Layered;
}

const LABEL_FADE_START = 0.7;
const SCROLL_BAND_TOP = 0.25;
const SCROLL_BAND_BOTTOM = 0.7;
const FIGURE_PAD = 12;

@Component({
	selector: 'app-whiteboard',
	changeDetection: ChangeDetectionStrategy.OnPush,
	imports: [NgTemplateOutlet],
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
	private readonly content = viewChild.required<ElementRef<HTMLElement>>('content');
	private readonly rowEls = viewChildren<ElementRef<HTMLElement>>('row');
	private readonly safe = new Map<string, SafeHtml>();

	protected readonly rows = computed<Row[]>(() => {
		const scene = this.scene();
		const layer = (section: number, name: Layer): Layered => {
			const items = scene.items.filter((i) => i.section === section && i.layer === name);
			return {
				highlights: items.filter((i): i is HighlightItem => i.type === 'highlight' && i.w > 0),
				content: items.filter((i) => i.type !== 'highlight'),
			};
		};
		return scene.sections.map((sec) => ({ sec, fig: sec.frame ? layer(sec.index, 'fig') : null, col: layer(sec.index, 'col') }));
	});
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
		const row = c ? this.rowEls()[c.section]?.nativeElement : undefined;
		const sec = c ? this.scene().sections[c.section] : undefined;
		const width = this.content().nativeElement.clientWidth;
		if (!c || !row || !sec || !this.follow() || width === 0) {
			return;
		}
		const scale = width / this.scene().width;
		const target = c.layer === 'col' ? this.columnTarget(el, row, sec, c.y, scale) : this.figureTarget(el, row, sec, scale);
		if (target !== null) {
			el.scrollTo({ top: Math.max(0, target), behavior: 'smooth' });
		}
	}

	private columnTarget(el: HTMLElement, row: HTMLElement, sec: SectionInfo, y: number, scale: number): number | null {
		const px = row.offsetTop + (y - sec.top) * scale;
		const view = el.clientHeight;
		const outside = px < el.scrollTop + view * SCROLL_BAND_TOP || px > el.scrollTop + view * SCROLL_BAND_BOTTOM;
		return outside ? px - view * 0.4 : null;
	}

	private figureTarget(el: HTMLElement, row: HTMLElement, sec: SectionInfo, scale: number): number | null {
		const figH = (sec.frame?.h ?? 0) * scale;
		const rowTop = row.offsetTop;
		const figTop = Math.min(Math.max(el.scrollTop + FIGURE_PAD, rowTop), rowTop + row.offsetHeight - figH);
		const visible = figTop >= el.scrollTop && figTop + figH <= el.scrollTop + el.clientHeight;
		return visible ? null : rowTop - FIGURE_PAD;
	}
}
