import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { authInterceptor } from './auth.interceptor';
import { TokenService } from './token.service';

describe('authInterceptor', () => {
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
    tokens.save({ accessToken: 'old', refreshToken: 'refresh-1' });
  });

  afterEach(() => backend.verify());

  it('adds the bearer token to API calls', () => {
    http.get('/api/v1/users/me').subscribe();
    const request = backend.expectOne('/api/v1/users/me');
    expect(request.request.headers.get('Authorization')).toBe('Bearer old');
    request.flush({});
  });

  it('refreshes once on a 401 and retries the call with the new token', () => {
    let result: unknown;
    http.get('/api/v1/users/me').subscribe(value => (result = value));
    backend.expectOne('/api/v1/users/me').flush({}, { status: 401, statusText: 'Unauthorized' });

    backend.expectOne('/api/v1/auth/refresh').flush({ accessToken: 'new', refreshToken: 'refresh-2' });
    const retry = backend.expectOne('/api/v1/users/me');
    expect(retry.request.headers.get('Authorization')).toBe('Bearer new');
    retry.flush({ id: 'u1' });
    expect(result).toEqual({ id: 'u1' });
  });

  it('ends the session when the refresh fails', () => {
    let failure: unknown;
    http.get('/api/v1/users/me').subscribe({ error: error => (failure = error) });
    backend.expectOne('/api/v1/users/me').flush({}, { status: 401, statusText: 'Unauthorized' });
    backend.expectOne('/api/v1/auth/refresh').flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(tokens.accessToken()).toBeNull();
    expect(router.navigate).toHaveBeenCalled();
    expect(failure).toBeTruthy();
  });
});
