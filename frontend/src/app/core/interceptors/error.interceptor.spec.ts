import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { ApiError } from '../errors/api-error';
import { errorInterceptor } from './error.interceptor';

describe('errorInterceptor', () => {
  it('turns an HTTP failure into an ApiError with the spec 4.7 code', () => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([errorInterceptor])), provideHttpClientTesting()],
    });
    const http = TestBed.inject(HttpClient);
    const backend = TestBed.inject(HttpTestingController);
    let caught: unknown;

    http.post('/api/v1/lessons/generate', {}).subscribe({ error: error => (caught = error) });
    backend.expectOne('/api/v1/lessons/generate').flush(
      { error: { code: 'DAILY_LIMIT_REACHED', message: 'Limit', traceId: 't1' } },
      { status: 429, statusText: 'Too Many Requests' },
    );

    expect(caught).toBeInstanceOf(ApiError);
    expect((caught as ApiError).code).toBe('DAILY_LIMIT_REACHED');
    expect((caught as ApiError).traceId).toBe('t1');
  });
});
