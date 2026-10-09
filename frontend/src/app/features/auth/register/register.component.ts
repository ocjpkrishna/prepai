import { Component, computed, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, FormGroup, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatCheckbox } from '@angular/material/checkbox';
import { MatError, MatFormField, MatHint, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { Router, RouterLink } from '@angular/router';
import { toSignal } from '@angular/core/rxjs-interop';
import { startWith } from 'rxjs';
import { ApiError } from '../../../core/errors/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { RegisterRequest } from '../../../core/models/user.model';
import { ApiErrorComponent } from '../../../shared/components/api-error/api-error.component';
import { isMinor } from '../age-gate';

const MIN_PASSWORD_LENGTH = 8;
const SERVER_ERROR_KEY = 'server';

@Component({
  selector: 'app-register',
  imports: [
    ReactiveFormsModule, RouterLink, MatButton, MatCheckbox, MatFormField, MatLabel, MatError, MatHint, MatInput,
    MatSelect, MatOption, ApiErrorComponent,
  ],
  templateUrl: './register.component.html',
  styleUrl: '../auth-form.scss',
})
export class RegisterComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly fb = inject(FormBuilder);

  protected readonly minPasswordLength = MIN_PASSWORD_LENGTH;
  protected readonly form: FormGroup = this.fb.group({
    name: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(MIN_PASSWORD_LENGTH)]],
    language: this.fb.nonNullable.control<'en' | 'hi'>('en'),
    dateOfBirth: ['', Validators.required],
    guardianEmail: ['', [Validators.email]],
    termsAccepted: [false, Validators.requiredTrue],
  });
  protected readonly submitting = signal(false);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly emailTaken = signal(false);

  private readonly dateOfBirth = toSignal(
    this.form.controls['dateOfBirth'].valueChanges.pipe(startWith(this.form.controls['dateOfBirth'].value)),
    { initialValue: '' },
  );
  protected readonly minor = computed(() => isMinor(this.dateOfBirth() ?? ''));

  protected submit(): void {
    this.form.markAllAsTouched();
    this.requireGuardianIfMinor();
    if (this.form.invalid) {
      return;
    }
    this.submitting.set(true);
    this.error.set(null);
    this.emailTaken.set(false);
    this.auth.register(this.toRequest()).subscribe({
      next: () => void this.router.navigate([this.minor() ? '/guardian-pending' : '/verify-email']),
      error: (error: ApiError) => this.onFailure(error),
    });
  }

  private requireGuardianIfMinor(): void {
    const guardian = this.form.controls['guardianEmail'];
    if (this.minor() && !guardian.value) {
      guardian.setErrors({ required: true });
    }
  }

  private toRequest(): RegisterRequest {
    const value = this.form.getRawValue();
    return {
      name: value.name,
      email: value.email,
      password: value.password,
      dateOfBirth: value.dateOfBirth,
      guardianEmail: this.minor() ? value.guardianEmail : null,
      termsAccepted: true,
      language: value.language,
    };
  }

  private onFailure(error: ApiError): void {
    this.submitting.set(false);
    if (error.code === 'EMAIL_ALREADY_EXISTS') {
      this.emailTaken.set(true);
      return;
    }
    if (error.code === 'VALIDATION_FAILED') {
      this.showFieldIssues(error);
      return;
    }
    this.error.set(error);
  }

  private showFieldIssues(error: ApiError): void {
    for (const control of Object.keys(this.form.controls)) {
      const issues = error.fieldIssues(control);
      if (issues.length > 0) {
        this.setServerError(this.form.controls[control], issues[0]);
      }
    }
  }

  private setServerError(control: AbstractControl, message: string): void {
    const errors: ValidationErrors = { [SERVER_ERROR_KEY]: message };
    control.setErrors(errors);
    control.markAsTouched();
  }
}
