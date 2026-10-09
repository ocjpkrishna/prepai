import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { AuthService } from '../../core/auth/auth.service';
import { UserService } from '../../core/services/user.service';
import { ProfileComponent } from './profile.component';

describe('ProfileComponent', () => {
  const user = { id: 'u', email: 'a@b.c', name: 'Asha', language: 'en', plan: 'FREE', isMinor: false, emailVerified: true, guardianConsentAt: null };
  let users: { me: ReturnType<typeof vi.fn>; deleteAccount: ReturnType<typeof vi.fn> };
  let auth: { logout: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    users = { me: vi.fn().mockReturnValue(of(user)), deleteAccount: vi.fn().mockReturnValue(of(undefined)) };
    auth = { logout: vi.fn() };
    TestBed.configureTestingModule({
      imports: [ProfileComponent],
      providers: [provideRouter([]), { provide: UserService, useValue: users }, { provide: AuthService, useValue: auth }],
    });
  });

  it('does not delete the account until DELETE is typed', () => {
    const fixture = TestBed.createComponent(ProfileComponent);
    fixture.detectChanges();
    fixture.componentInstance['deleteAccount']();
    expect(users.deleteAccount).not.toHaveBeenCalled();

    fixture.componentInstance['deleteConfirmation'].setValue('DELETE');
    fixture.componentInstance['deleteAccount']();
    expect(users.deleteAccount).toHaveBeenCalled();
    expect(auth.logout).toHaveBeenCalled();
  });

  it('shows the signed-in student\'s name and plan', () => {
    const fixture = TestBed.createComponent(ProfileComponent);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Plan: FREE');
  });
});
