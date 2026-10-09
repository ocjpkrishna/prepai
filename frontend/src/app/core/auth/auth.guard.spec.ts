import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { authGuard } from './auth.guard';
import { TokenService } from './token.service';

describe('authGuard', () => {
  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()],
    });
  });

  function run(url: string): boolean | UrlTree {
    const state = { url } as Parameters<typeof authGuard>[1];
    return TestBed.runInInjectionContext(() => authGuard({} as never, state)) as boolean | UrlTree;
  }

  it('lets a logged-in student through', () => {
    TestBed.inject(TokenService).save({ accessToken: 'a', refreshToken: 'r' });
    expect(run('/dashboard')).toBe(true);
  });

  it('sends a logged-out student to login with a return URL', () => {
    const result = run('/profile') as UrlTree;
    expect(TestBed.inject(Router).serializeUrl(result)).toBe('/login?returnUrl=%2Fprofile');
  });
});
