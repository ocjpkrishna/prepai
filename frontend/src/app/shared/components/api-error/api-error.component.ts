import { Component, computed, effect, input, output, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../../core/errors/api-error';
import { ERROR_COPY, RETRYABLE_CODES } from '../../../core/errors/error-copy';

const ONE_SECOND_MS = 1000;
const DEFAULT_RETRY_SECONDS = 60;

/** Shows one API error in its friendly form: copy, Retry, upgrade prompt or a countdown (spec 4.7). */
@Component({
  selector: 'app-api-error',
  imports: [MatButton, RouterLink],
  templateUrl: './api-error.component.html',
  styleUrl: './api-error.component.scss',
})
export class ApiErrorComponent {
  readonly error = input<ApiError | null>(null);
  readonly retry = output<void>();

  protected readonly remaining = signal(0);
  protected readonly message = computed(() => {
    const error = this.error();
    return error ? ERROR_COPY[error.code] : '';
  });
  protected readonly canRetry = computed(() => {
    const error = this.error();
    return !!error && RETRYABLE_CODES.includes(error.code) && this.remaining() === 0;
  });
  protected readonly isDailyLimit = computed(() => this.error()?.code === 'DAILY_LIMIT_REACHED');
  protected readonly isRateLimited = computed(() => this.error()?.code === 'RATE_LIMITED');

  constructor() {
    effect(onCleanup => {
      const timer = this.startCountdown(this.error());
      onCleanup(() => clearInterval(timer));
    });
  }

  private startCountdown(error: ApiError | null): ReturnType<typeof setInterval> | undefined {
    if (error?.code !== 'RATE_LIMITED') {
      this.remaining.set(0);
      return undefined;
    }
    this.remaining.set(error.retryAfterSeconds ?? DEFAULT_RETRY_SECONDS);
    return setInterval(() => this.remaining.update(seconds => Math.max(0, seconds - 1)), ONE_SECOND_MS);
  }
}
