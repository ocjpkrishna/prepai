import { ChangeDetectionStrategy, Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { LessonSource, LessonSummary } from '../../core/services/lesson-source';

interface Mode {
	id: string;
	title: string;
	hint: string;
}

const MODES: Mode[] = [
	{ id: 'cram', title: 'Cram for an exam', hint: 'Learn it fast, then quiz me so it sticks' },
	{ id: 'homework', title: 'Homework help', hint: 'Work a problem through together, not just the answer' },
	{ id: 'simple', title: 'Explain it simply', hint: 'From scratch, no assumed knowledge, lots of drawing' },
	{ id: 'practice', title: 'Practice problems', hint: 'Work through problems together, one at a time' },
];

@Component({
	selector: 'app-home',
	changeDetection: ChangeDetectionStrategy.OnPush,
	imports: [FormsModule, RouterLink],
	templateUrl: './home.component.html',
	styleUrl: './home.component.scss',
})
export class HomeComponent implements OnInit {
	private readonly source = inject(LessonSource);
	private readonly router = inject(Router);

	protected readonly modes = MODES;
	protected readonly mode = signal('simple');
	protected readonly prompt = signal('');
	protected readonly busy = signal(false);
	protected readonly samples = signal<LessonSummary[]>([]);

	async ngOnInit(): Promise<void> {
		this.samples.set(await this.source.list());
	}

	protected async start(): Promise<void> {
		const text = this.prompt().trim();
		if (!text || this.busy()) {
			return;
		}
		this.busy.set(true);
		const lesson = await this.source.generate(text);
		await this.router.navigate(['/lesson', lesson.lessonId]);
	}

	protected onKey(event: KeyboardEvent): void {
		if (event.key === 'Enter' && !event.shiftKey) {
			event.preventDefault();
			void this.start();
		}
	}
}
