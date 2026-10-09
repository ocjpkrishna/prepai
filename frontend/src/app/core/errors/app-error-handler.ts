import { ErrorHandler, inject, Injectable } from '@angular/core';
import { ErrorBoundaryService } from './error-boundary.service';

/** Catches errors no page handled and hands them to the global error boundary. */
@Injectable()
export class AppErrorHandler implements ErrorHandler {
  private readonly boundary = inject(ErrorBoundaryService);

  handleError(error: unknown): void {
    this.boundary.report(error);
  }
}
