import { Component, inject, input, signal } from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatError, MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { Router, RouterLink } from '@angular/router';
import { ApiError } from '../../../core/errors/api-error';
import { blockingErrorRoute } from '../../../core/errors/blocking-error-route';
import { AuthService } from '../../../core/auth/auth.service';
import { ApiErrorComponent } from '../../../shared/components/api-error/api-error.component';
import { safeReturnUrl } from '../return-url';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule, RouterLink, MatButton, MatFormField, MatLabel, MatError, MatInput, ApiErrorComponent],
  templateUrl: './login.component.html',
  styleUrl: '../auth-form.scss',
})
export class LoginComponent {
  readonly returnUrl = input<string | undefined>(undefined);

  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', Validators.required],
  });
  protected readonly submitting = signal(false);
  protected readonly invalidCredentials = signal(false);
  protected readonly error = signal<ApiError | null>(null);

  protected submit(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    const { email, password } = this.form.getRawValue();
    this.auth.login(email, password).subscribe({
      next: () => this.afterLogin(),
      error: (error: ApiError) => this.onFailure(error),
    });
  }

  private afterLogin(): void {
    void this.router.navigateByUrl(safeReturnUrl(this.returnUrl()));
  }

  private onFailure(error: ApiError): void {
    this.submitting.set(false);
    this.invalidCredentials.set(error.code === 'UNAUTHENTICATED');
    if (error.code !== 'UNAUTHENTICATED') {
      this.error.set(error);
    }
    const route = blockingErrorRoute(error);
    if (route) {
      void this.router.navigate([route]);
    }
  }
}
