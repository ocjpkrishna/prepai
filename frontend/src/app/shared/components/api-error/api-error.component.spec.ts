import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ApiError } from '../../../core/errors/api-error';
import { ApiErrorComponent } from './api-error.component';

describe('ApiErrorComponent', () => {
  beforeEach(() => {
    vi.useFakeTimers();
    TestBed.configureTestingModule({ imports: [ApiErrorComponent], providers: [provideRouter([])] });
  });

  afterEach(() => vi.useRealTimers());

  function render(error: ApiError | null) {
    const fixture = TestBed.createComponent(ApiErrorComponent);
    fixture.componentRef.setInput('error', error);
    fixture.detectChanges();
    return fixture;
  }

  it('offers Retry for a transient lesson failure, with the no-session-used copy', () => {
    const fixture = render(new ApiError({ status: 502, code: 'LESSON_GENERATION_FAILED', message: '' }));
    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('No session was used');
    expect(element.querySelector('button')?.textContent).toContain('Retry');
  });

  it('offers an upgrade link instead of Retry when the daily limit is reached', () => {
    const fixture = render(new ApiError({ status: 429, code: 'DAILY_LIMIT_REACHED', message: '' }));
    const element = fixture.nativeElement as HTMLElement;
    expect(element.querySelector('a[href="/pricing"]')).toBeTruthy();
    expect(element.textContent).not.toContain('Retry');
  });

  it('counts down the retry time for a rate-limited request', () => {
    const fixture = render(new ApiError({ status: 429, code: 'RATE_LIMITED', message: '', retryAfterSeconds: 3 }));
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Try again in 3s');
    vi.advanceTimersByTime(2000);
    fixture.detectChanges();
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Try again in 1s');
  });

  it('shows the trace reference for unexpected errors', () => {
    const fixture = render(new ApiError({ status: 500, code: 'INTERNAL_ERROR', message: '', traceId: 'trace42' }));
    expect((fixture.nativeElement as HTMLElement).textContent).toContain('Reference: trace42');
  });
});
