import { DatePipe } from '@angular/common';
import { Component, inject, OnInit, signal } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { ApiError } from '../../core/errors/api-error';
import { LessonHistoryPage, LessonSummary } from '../../core/models/lesson.model';
import { Usage, User } from '../../core/models/user.model';
import { LessonService } from '../../core/services/lesson.service';
import { UserService } from '../../core/services/user.service';
import { ApiErrorComponent } from '../../shared/components/api-error/api-error.component';
import { SubjectIconComponent } from '../../shared/components/subject-icon/subject-icon.component';
import { SUBJECT_OPTIONS } from '../../shared/subjects';

interface DashboardData {
  user: User;
  usage: Usage;
  history: LessonHistoryPage;
}

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, DatePipe, MatButton, ApiErrorComponent, SubjectIconComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly users = inject(UserService);
  private readonly lessons = inject(LessonService);

  protected readonly subjects = SUBJECT_OPTIONS;
  protected readonly user = signal<User | null>(null);
  protected readonly usage = signal<Usage | null>(null);
  protected readonly history = signal<LessonSummary[] | null>(null);
  protected readonly error = signal<ApiError | null>(null);

  ngOnInit(): void {
    this.load();
  }

  protected load(): void {
    this.error.set(null);
    forkJoin({
      user: this.users.me(),
      usage: this.users.usage(),
      history: this.lessons.history(),
    }).subscribe({
      next: data => this.show(data),
      error: (error: ApiError) => this.error.set(error),
    });
  }

  protected usageText(usage: Usage): string {
    if (usage.sessionLimit === null) {
      return `${usage.sessionsToday} sessions today. Unlimited on your plan.`;
    }
    return `${usage.sessionsToday} of ${usage.sessionLimit} sessions used today`;
  }


  private show(data: DashboardData): void {
    this.user.set(data.user);
    this.usage.set(data.usage);
    this.history.set(data.history.content);
  }
}
