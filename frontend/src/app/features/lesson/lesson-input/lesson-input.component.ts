import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, FormControl, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { Router } from '@angular/router';
import { ApiError, ErrorCode } from '../../../core/errors/api-error';
import { blockingErrorRoute } from '../../../core/errors/blocking-error-route';
import { ERROR_COPY } from '../../../core/errors/error-copy';
import { Difficulty, Exam, ExtractResult, LessonInputType, LessonRequest, Subject } from '../../../core/models/lesson.model';
import { LessonService } from '../../../core/services/lesson.service';
import { ApiErrorComponent } from '../../../shared/components/api-error/api-error.component';
import { LoadingSpinnerComponent } from '../../../shared/components/loading-spinner/loading-spinner.component';
import { downscaleImage, isSupportedImage, MAX_IMAGE_BYTES, MAX_IMAGE_EDGE_PX } from './image-prep';
import { DIFFICULTY_OPTIONS, EXAM_OPTIONS, MAX_TEXT_LENGTH } from './lesson-input.options';
import { SUBJECT_OPTIONS } from '../../../shared/subjects';

@Component({
  selector: 'app-lesson-input',
  imports: [
    ReactiveFormsModule, MatButton, MatFormField, MatLabel, MatError, MatInput, MatSelect, MatOption,
    ApiErrorComponent, LoadingSpinnerComponent,
  ],
  templateUrl: './lesson-input.component.html',
  styleUrl: './lesson-input.component.scss',
})
export class LessonInputComponent implements OnInit {
  /** Set by the dashboard's quick-start picker, as ?subject=PHYSICS. */
  readonly subject = input<Subject | undefined>(undefined);

  private readonly lessons = inject(LessonService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly subjects = SUBJECT_OPTIONS;
  protected readonly exams = EXAM_OPTIONS;
  protected readonly difficulties = DIFFICULTY_OPTIONS;
  protected readonly maxTextLength = MAX_TEXT_LENGTH;

  protected readonly form = this.fb.nonNullable.group({
    type: this.fb.nonNullable.control<'TOPIC' | 'PROBLEM'>('PROBLEM'),
    subject: this.fb.nonNullable.control<Subject>('PHYSICS'),
    exam: this.fb.nonNullable.control<Exam>('JEE_MAIN'),
    difficulty: this.fb.nonNullable.control<Difficulty>('MEDIUM'),
    language: this.fb.nonNullable.control<'en' | 'hi'>('en'),
    text: this.fb.nonNullable.control('', [Validators.required, Validators.maxLength(MAX_TEXT_LENGTH)]),
  });
  /** The editable "We read this as…" box, filled from the photo. */
  protected readonly readText = new FormControl('', { nonNullable: true });

  protected readonly status = signal<'idle' | 'reading' | 'reviewing' | 'generating'>('idle');
  protected readonly error = signal<ApiError | null>(null);
  protected readonly extracted = signal<ExtractResult | null>(null);

  ngOnInit(): void {
    const subject = this.subject();
    if (subject) {
      this.form.patchValue({ subject });
    }
  }

  protected submitText(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { type, text } = this.form.getRawValue();
    this.generate(type, text);
  }

  protected async onPhotoSelected(event: Event): Promise<void> {
    const file = (event.target as HTMLInputElement).files?.[0];
    if (!file) {
      return;
    }
    if (!isSupportedImage(file.type)) {
      this.showClientError('IMAGE_UNSUPPORTED');
      return;
    }
    await this.preparePhoto(file);
  }

  /** Confirms the reading the student checked and submits it as a PROBLEM (spec agent7, task 12). */
  protected confirmReading(): void {
    const text = this.readText.value.trim();
    if (!text) {
      return;
    }
    this.generate('PROBLEM', text);
  }

  /** Retry repeats the last action: the confirmed reading if there is one, otherwise the typed question. */
  protected retry(): void {
    if (this.extracted()) {
      this.confirmReading();
      return;
    }
    this.submitText();
  }

  protected retakePhoto(): void {
    this.extracted.set(null);
    this.error.set(null);
    this.status.set('idle');
  }

  private async preparePhoto(file: File): Promise<void> {
    this.status.set('reading');
    this.error.set(null);
    const prepared = await downscaleImage(file, MAX_IMAGE_EDGE_PX);
    if (prepared.size > MAX_IMAGE_BYTES) {
      this.showClientError('IMAGE_TOO_LARGE');
      return;
    }
    this.lessons.extract(prepared).subscribe({
      next: result => this.showReading(result),
      error: (error: ApiError) => this.onFailure(error),
    });
  }

  private showReading(result: ExtractResult): void {
    this.extracted.set(result);
    this.readText.setValue(result.problemText);
    this.status.set('reviewing');
  }

  private generate(type: LessonInputType, text: string): void {
    this.status.set('generating');
    this.error.set(null);
    this.lessons.generate(this.toRequest(type, text)).subscribe({
      next: lesson => void this.router.navigate(['/lessons', lesson.lessonId]),
      error: (error: ApiError) => this.onFailure(error),
    });
  }

  private toRequest(type: LessonInputType, text: string): LessonRequest {
    const value = this.form.getRawValue();
    return {
      type,
      subject: value.subject,
      exam: value.exam,
      input: { text, imageBase64: null },
      difficulty: value.difficulty,
      language: value.language,
    };
  }

  private onFailure(error: ApiError): void {
    this.status.set(this.extracted() ? 'reviewing' : 'idle');
    const route = blockingErrorRoute(error);
    if (route) {
      void this.router.navigate([route]);
      return;
    }
    this.error.set(error);
    this.applyFieldIssues(error);
  }

  private applyFieldIssues(error: ApiError): void {
    const issues = error.fieldIssues('input.text');
    if (issues.length > 0) {
      this.form.controls.text.setErrors({ server: issues[0] });
    }
  }

  private showClientError(code: ErrorCode): void {
    this.status.set('idle');
    this.error.set(new ApiError({ status: 0, code, message: ERROR_COPY[code] }));
  }
}
