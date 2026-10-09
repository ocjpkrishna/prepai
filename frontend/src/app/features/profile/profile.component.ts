import { Component, inject, OnInit, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';
import { MatFormField, MatLabel } from '@angular/material/form-field';
import { MatInput } from '@angular/material/input';
import { MatOption, MatSelect } from '@angular/material/select';
import { AuthService } from '../../core/auth/auth.service';
import { ApiError } from '../../core/errors/api-error';
import { User } from '../../core/models/user.model';
import { UserService } from '../../core/services/user.service';
import { ApiErrorComponent } from '../../shared/components/api-error/api-error.component';

export const DELETE_CONFIRMATION = 'DELETE';
const EXPORT_FILE_NAME = 'prepai-my-data.json';

@Component({
  selector: 'app-profile',
  imports: [ReactiveFormsModule, MatButton, MatFormField, MatLabel, MatInput, MatSelect, MatOption, ApiErrorComponent],
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.scss',
})
export class ProfileComponent implements OnInit {
  private readonly users = inject(UserService);
  private readonly auth = inject(AuthService);

  protected readonly deleteWord = DELETE_CONFIRMATION;
  protected readonly user = signal<User | null>(null);
  protected readonly message = signal<string | null>(null);
  protected readonly error = signal<ApiError | null>(null);
  protected readonly language = new FormControl<'en' | 'hi'>('en', { nonNullable: true });
  protected readonly deleteConfirmation = new FormControl('', { nonNullable: true });

  ngOnInit(): void {
    this.users.me().subscribe({
      next: user => this.show(user),
      error: (error: ApiError) => this.error.set(error),
    });
  }

  protected saveLanguage(): void {
    this.users.savePreferences(this.language.value).subscribe({
      next: () => this.message.set('Saved.'),
      error: (error: ApiError) => this.error.set(error),
    });
  }

  protected downloadData(): void {
    this.users.exportData().subscribe({
      next: blob => this.saveFile(blob),
      error: (error: ApiError) => this.error.set(error),
    });
  }

  protected deleteAccount(): void {
    if (this.deleteConfirmation.value !== DELETE_CONFIRMATION) {
      return;
    }
    this.users.deleteAccount().subscribe({
      next: () => this.auth.logout(),
      error: (error: ApiError) => this.error.set(error),
    });
  }

  private show(user: User): void {
    this.user.set(user);
    this.language.setValue(user.language);
  }

  private saveFile(blob: Blob): void {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = EXPORT_FILE_NAME;
    link.click();
    URL.revokeObjectURL(url);
  }
}
