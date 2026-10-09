import { HttpErrorResponse, HttpHeaders } from '@angular/common/http';
import { ApiError, toApiError } from './api-error';
import { ERROR_COPY, RETRYABLE_CODES } from './error-copy';

const SPEC_CODES = [
  'VALIDATION_FAILED', 'UNAUTHENTICATED', 'FORBIDDEN', 'EMAIL_NOT_VERIFIED', 'CONSENT_REQUIRED',
  'NOT_FOUND', 'EMAIL_ALREADY_EXISTS', 'IMAGE_TOO_LARGE', 'IMAGE_UNSUPPORTED', 'IMAGE_UNREADABLE',
  'PROBLEM_OUT_OF_SCOPE', 'DAILY_LIMIT_REACHED', 'RATE_LIMITED', 'INTERNAL_ERROR',
  'LESSON_GENERATION_FAILED', 'LLM_UNAVAILABLE', 'TTS_UNAVAILABLE',
];

function httpError(status: number, body: unknown = null, traceHeader?: string): HttpErrorResponse {
  const headers = new HttpHeaders(traceHeader ? { 'X-Trace-Id': traceHeader } : {});
  return new HttpErrorResponse({ status, error: body, headers });
}

describe('toApiError', () => {
  it('reads the spec 4.7 error body, including details, retry time and trace ID', () => {
    const body = {
      error: {
        code: 'VALIDATION_FAILED',
        message: 'Bad input',
        details: [{ field: 'input.text', issue: 'must not be blank' }],
        retryAfterSeconds: 30,
        traceId: 'abc123',
      },
    };
    const error = toApiError(httpError(400, body));
    expect(error).toBeInstanceOf(ApiError);
    expect(error.code).toBe('VALIDATION_FAILED');
    expect(error.fieldIssues('input.text')).toEqual(['must not be blank']);
    expect(error.retryAfterSeconds).toBe(30);
    expect(error.traceId).toBe('abc123');
  });

  it('falls back to the status when the body is not in the error shape', () => {
    expect(toApiError(httpError(429)).code).toBe('RATE_LIMITED');
    expect(toApiError(httpError(500, '<html>')).code).toBe('INTERNAL_ERROR');
  });

  it('uses the X-Trace-Id header when the body has no trace ID', () => {
    expect(toApiError(httpError(500, null, 'hdr999')).traceId).toBe('hdr999');
  });

  it('reports a network failure with no response as NETWORK_UNAVAILABLE', () => {
    expect(toApiError(httpError(0)).code).toBe('NETWORK_UNAVAILABLE');
  });
});

describe('error copy', () => {
  it('has student-facing copy for every code in spec 4.7', () => {
    for (const code of SPEC_CODES) {
      expect(ERROR_COPY[code as keyof typeof ERROR_COPY], code).toBeTruthy();
    }
  });

  it('allows Retry only for transient codes', () => {
    expect(RETRYABLE_CODES).toContain('LLM_UNAVAILABLE');
    expect(RETRYABLE_CODES).toContain('LESSON_GENERATION_FAILED');
    expect(RETRYABLE_CODES).not.toContain('DAILY_LIMIT_REACHED');
  });
});
