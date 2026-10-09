import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ApiError } from '../../../core/errors/api-error';
import { AuthService } from '../../../core/auth/auth.service';
import { RegisterComponent } from './register.component';

describe('RegisterComponent', () => {
  let auth: { register: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(() => {
    auth = { register: vi.fn().mockReturnValue(of(undefined)) };
    TestBed.configureTestingModule({
      imports: [RegisterComponent],
      providers: [provideRouter([]), { provide: AuthService, useValue: auth }],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function fill(fixture: ReturnType<typeof TestBed.createComponent<RegisterComponent>>, dateOfBirth: string) {
    fixture.componentInstance['form'].patchValue({
      name: 'Asha',
      email: 'asha@example.com',
      password: 'longenough',
      dateOfBirth,
      termsAccepted: true,
    });
    fixture.detectChanges();
  }

  it('asks for a guardian email when the student is under 18, and blocks submit without it', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    fill(fixture, '2015-01-01');
    expect(fixture.componentInstance['minor']()).toBe(true);

    fixture.componentInstance['submit']();
    expect(auth.register).not.toHaveBeenCalled();
    expect(fixture.componentInstance['form'].controls['guardianEmail'].hasError('required')).toBe(true);
  });

  it('sends a minor to the guardian-pending screen after registering', () => {
    auth.register.mockReturnValue(of(undefined));
    const fixture = TestBed.createComponent(RegisterComponent);
    fill(fixture, '2015-01-01');
    fixture.componentInstance['form'].controls['guardianEmail'].setValue('parent@example.com');

    fixture.componentInstance['submit']();
    expect(auth.register).toHaveBeenCalledWith(expect.objectContaining({ guardianEmail: 'parent@example.com' }));
    expect(router.navigate).toHaveBeenCalledWith(['/guardian-pending']);
  });

  it('sends an adult to the verify-email screen and sends no guardian email', () => {
    const fixture = TestBed.createComponent(RegisterComponent);
    fill(fixture, '1990-01-01');

    fixture.componentInstance['submit']();
    expect(auth.register).toHaveBeenCalledWith(expect.objectContaining({ guardianEmail: null, termsAccepted: true }));
    expect(router.navigate).toHaveBeenCalledWith(['/verify-email']);
  });

  it('shows a login link when the email is already registered', () => {
    auth.register.mockReturnValue(throwError(() => new ApiError({ status: 409, code: 'EMAIL_ALREADY_EXISTS', message: '' })));
    const fixture = TestBed.createComponent(RegisterComponent);
    fill(fixture, '1990-01-01');

    fixture.componentInstance['submit']();
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).querySelector('a[href="/login"]')).toBeTruthy();
  });

  it('maps VALIDATION_FAILED details onto the matching field', () => {
    const details = [{ field: 'password', issue: 'is too common' }];
    auth.register.mockReturnValue(throwError(() => new ApiError({ status: 400, code: 'VALIDATION_FAILED', message: '', details })));
    const fixture = TestBed.createComponent(RegisterComponent);
    fill(fixture, '1990-01-01');

    fixture.componentInstance['submit']();
    expect(fixture.componentInstance['form'].controls['password'].errors).toEqual({ server: 'is too common' });
  });
});
