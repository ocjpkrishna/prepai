import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { LessonService } from '../../core/services/lesson.service';
import { UserService } from '../../core/services/user.service';
import { DashboardComponent } from './dashboard.component';

describe('DashboardComponent', () => {
  const user = { id: 'u', email: 'a@b.c', name: 'Asha', language: 'en', plan: 'FREE', isMinor: false, emailVerified: true, guardianConsentAt: null };
  const usage = { plan: 'FREE', sessionsToday: 2, sessionLimit: 3, maxSessionMinutes: 5 };
  const history = { content: [{ lessonId: 'l1', title: 'Projectile motion', subject: 'PHYSICS', createdAt: '2026-10-08T10:00:00Z' }], page: 0, totalPages: 1 };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [DashboardComponent],
      providers: [
        provideRouter([]),
        { provide: UserService, useValue: { me: () => of(user), usage: () => of(usage) } },
        { provide: LessonService, useValue: { history: () => of(history) } },
      ],
    });
  });

  it('shows today\'s usage against the plan limit and the lesson history', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('2 of 3 sessions used today');
    expect(element.textContent).toContain('Projectile motion');
    expect(element.querySelector('a[href="/pricing"]')).toBeTruthy();
  });

  it('offers a quick start link for each subject', () => {
    const fixture = TestBed.createComponent(DashboardComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelectorAll('a[href^="/lessons/new"]').length).toBe(3);
  });
});
