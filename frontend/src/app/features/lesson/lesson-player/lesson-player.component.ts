import { Component, DestroyRef, effect, inject, input, OnInit, signal, untracked } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatButtonToggle, MatButtonToggleGroup } from '@angular/material/button-toggle';
import { MatSnackBar } from '@angular/material/snack-bar';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../../core/errors/api-error';
import { PLAYBACK_SPEEDS } from '../../../core/models/tts.model';
import { LessonService } from '../../../core/services/lesson.service';
import { TtsService } from '../../../core/services/tts.service';
import { ApiErrorComponent } from '../../../shared/components/api-error/api-error.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { WhiteboardComponent } from '../whiteboard/whiteboard.component';
import { LessonPlaybackService } from './lesson-playback.service';

const SILENT_NOTICE = 'Voice is unavailable, showing captions';
const SILENT_NOTICE_MS = 5000;

/** The lesson page: loads a lesson and plays it on the whiteboard with narration and captions. */
@Component({
  selector: 'app-lesson-player',
  imports: [
    MatButton, MatButtonToggleGroup, MatButtonToggle, RouterLink,
    WhiteboardComponent, ApiErrorComponent, LoadingSpinnerComponent,
  ],
  templateUrl: './lesson-player.component.html',
  styleUrl: './lesson-player.component.scss',
  providers: [LessonPlaybackService],
})
export class LessonPlayerComponent implements OnInit {
  /** From the route /lessons/:lessonId. */
  readonly lessonId = input.required<string>();

  protected readonly playback = inject(LessonPlaybackService);
  protected readonly tts = inject(TtsService);
  protected readonly speeds = PLAYBACK_SPEEDS;
  protected readonly error = signal<ApiError | null>(null);
  protected readonly loading = signal(true);

  private readonly lessons = inject(LessonService);
  private readonly snackBar = inject(MatSnackBar);

  constructor() {
    inject(DestroyRef).onDestroy(() => this.playback.stop());
    effect(() => {
      if (!this.tts.available()) {
        untracked(() => this.snackBar.open(SILENT_NOTICE, undefined, { duration: SILENT_NOTICE_MS }));
      }
    });
  }

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.loading.set(true);
    this.error.set(null);
    this.lessons.get(this.lessonId()).subscribe({
      next: (lesson) => {
        this.playback.load(lesson);
        this.loading.set(false);
      },
      error: (error: ApiError) => {
        this.error.set(error);
        this.loading.set(false);
      },
    });
  }

  protected togglePause(): void {
    if (this.playback.paused()) {
      this.playback.resume();
    } else {
      this.playback.pause();
    }
  }
}
