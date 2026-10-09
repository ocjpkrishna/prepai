import { Component, inject, input, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { ApiError } from '../../../core/errors/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiErrorComponent } from '../../../shared/components/api-error/api-error.component';

/** Without a token it tells the student to check their inbox; with one (the email link) it verifies it. */
@Component({
  selector: 'app-verify-email',
  imports: [RouterLink, MatButton, ApiErrorComponent],
  templateUrl: './verify-email.component.html',
  styleUrl: '../auth-form.scss',
})
export class VerifyEmailComponent {
  readonly token = input<string | undefined>(undefined);

  private readonly auth = inject(AuthService);

  protected readonly verified = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  protected verify(): void {
    const token = this.token();
    if (!token) {
      return;
    }
    this.error.set(null);
    this.auth.verifyEmail(token).subscribe({
      next: () => this.verified.set(true),
      error: (error: ApiError) => this.error.set(error),
    });
  }
}
