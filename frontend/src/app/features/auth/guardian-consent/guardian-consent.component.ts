import { Component, inject, input, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../../core/errors/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiErrorComponent } from '../../../shared/components/api-error/api-error.component';

/** The public page a guardian opens from the consent email. */
@Component({
  selector: 'app-guardian-consent',
  imports: [RouterLink, MatButton, ApiErrorComponent],
  templateUrl: './guardian-consent.component.html',
  styleUrl: '../auth-form.scss',
})
export class GuardianConsentComponent {
  readonly token = input<string | undefined>(undefined);

  private readonly auth = inject(AuthService);

  protected readonly confirmed = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  protected confirm(): void {
    const token = this.token();
    if (!token) {
      return;
    }
    this.error.set(null);
    this.auth.confirmGuardianConsent(token).subscribe({
      next: () => this.confirmed.set(true),
      error: (error: ApiError) => this.error.set(error),
    });
  }
}
