import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router, UrlTree } from '@angular/router';
import { authGuard } from './auth.guard';
import { authInterceptor } from './auth.interceptor';
import { TokenService } from './token.service';

const DASHBOARD_URL = '/api/v1/users/me';
const REFRESH_URL = '/api/v1/auth/refresh';

describe('auth flow: guard and interceptor together', () => {
  let http: HttpClient;
  let backend: HttpTestingController;
  let tokens: TokenService;
  let router: Router;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideRouter([]),
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    http = TestBed.inject(HttpClient);
    backend = TestBed.inject(HttpTestingController);
    tokens = TestBed.inject(TokenService);
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  afterEach(() => backend.verify());

  function guardFor(url: string): boolean | UrlTree {
    const state = { url } as Parameters<typeof authGuard>[1];
    return TestBed.runInInjectionContext(() => authGuard({} as never, state)) as boolean | UrlTree;
  }

  it('keeps a student whose refresh succeeds inside the protected area', () => {
    tokens.save({ accessToken: 'old', refreshToken: 'refresh-1' });
    http.get(DASHBOARD_URL).subscribe();
    backend.expectOne(DASHBOARD_URL).flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne(REFRESH_URL).flush({ accessToken: 'new', refreshToken: 'refresh-2' });
    backend.expectOne(DASHBOARD_URL).flush({});

    expect(guardFor('/dashboard')).toBe(true);
  });

  it('sends a student whose refresh fails to login, and the guard blocks them after', () => {
    tokens.save({ accessToken: 'old', refreshToken: 'refresh-1' });
    http.get(DASHBOARD_URL).subscribe({ error: () => undefined });
    backend.expectOne(DASHBOARD_URL).flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne(REFRESH_URL).flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(router.navigate).toHaveBeenCalledWith(['/login'], expect.anything());
    expect(TestBed.inject(Router).serializeUrl(guardFor('/profile') as UrlTree)).toBe(
      '/login?returnUrl=%2Fprofile',
    );
  });

  it('sends no bearer header and no refresh once the session has ended', () => {
    tokens.save({ accessToken: 'old', refreshToken: 'refresh-1' });
    tokens.clear();
    http.get(DASHBOARD_URL).subscribe({ error: () => undefined });
    const request = backend.expectOne(DASHBOARD_URL);

    expect(request.request.headers.has('Authorization')).toBe(false);
    request.flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne(REFRESH_URL).flush({}, { status: 401, statusText: 'Unauthorized' });
  });
});
