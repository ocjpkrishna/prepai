import { Injectable, signal } from '@angular/core';
import { ApiError } from './api-error';

const REFERENCE_LENGTH = 12;

/** Holds the reference shown by the global error boundary after an unexpected failure. */
@Injectable({ providedIn: 'root' })
export class ErrorBoundaryService {
  readonly reference = signal<string | null>(null);

  report(error: unknown): void {
    console.error(error);
    this.reference.set(referenceFor(error));
  }

  dismiss(): void {
    this.reference.set(null);
  }
}

/** Reuses the server's trace ID when there is one, so support can match it to the logs. */
function referenceFor(error: unknown): string {
  if (error instanceof ApiError && error.traceId) {
    return error.traceId;
  }
  return crypto.randomUUID().replace(/-/g, '').slice(0, REFERENCE_LENGTH);
}
