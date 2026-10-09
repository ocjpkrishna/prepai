import { HttpErrorResponse } from '@angular/common/http';
import { ApiErrorBody, ApiErrorCode, FieldIssue } from './api-error.model';

/** Codes the client raises itself, when no response arrived from the server. */
export type ErrorCode = ApiErrorCode | 'NETWORK_UNAVAILABLE';

export interface ApiErrorInit {
  status: number;
  code: ErrorCode;
  message: string;
  details?: FieldIssue[];
  retryAfterSeconds?: number;
  traceId?: string;
}

export class ApiError extends Error {
  readonly status: number;
  readonly code: ErrorCode;
  readonly details: FieldIssue[];
  readonly retryAfterSeconds?: number;
  readonly traceId?: string;

  constructor(init: ApiErrorInit) {
    super(init.message);
    this.name = 'ApiError';
    this.status = init.status;
    this.code = init.code;
    this.details = init.details ?? [];
    this.retryAfterSeconds = init.retryAfterSeconds;
    this.traceId = init.traceId;
  }

  /** Field issues for one form field, used for inline errors. */
  fieldIssues(field: string): string[] {
    return this.details.filter(detail => detail.field === field).map(detail => detail.issue);
  }
}

const DEFAULT_CODE_BY_STATUS: Partial<Record<number, ErrorCode>> = {
  400: 'VALIDATION_FAILED',
  401: 'UNAUTHENTICATED',
  403: 'FORBIDDEN',
  404: 'NOT_FOUND',
  429: 'RATE_LIMITED',
  503: 'LLM_UNAVAILABLE',
};

export function toApiError(response: HttpErrorResponse): ApiError {
  if (response.status === 0) {
    return new ApiError({ status: 0, code: 'NETWORK_UNAVAILABLE', message: 'No connection' });
  }
  const body = readBody(response);
  const traceId = body?.traceId ?? response.headers?.get('X-Trace-Id') ?? undefined;
  return new ApiError({
    status: response.status,
    code: body?.code ?? DEFAULT_CODE_BY_STATUS[response.status] ?? 'INTERNAL_ERROR',
    message: body?.message ?? '',
    details: body?.details,
    retryAfterSeconds: body?.retryAfterSeconds,
    traceId,
  });
}

function readBody(response: HttpErrorResponse): ApiErrorBody['error'] | undefined {
  const body = response.error as ApiErrorBody | null;
  return body?.error;
}
