import { Component, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { ErrorBoundaryService } from '../../../core/errors/error-boundary.service';

@Component({
  selector: 'app-error-boundary',
  imports: [MatButton],
  templateUrl: './error-boundary.component.html',
  styleUrl: './error-boundary.component.scss',
})
export class ErrorBoundaryComponent {
  protected readonly boundary = inject(ErrorBoundaryService);

  protected async copyReference(reference: string): Promise<void> {
    await navigator.clipboard?.writeText(reference);
  }
}
