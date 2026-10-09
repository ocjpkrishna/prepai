/** Error codes from spec 4.7, as the backend sends them in the error body. */
export type ApiErrorCode =
  | 'VALIDATION_FAILED'
  | 'UNAUTHENTICATED'
  | 'FORBIDDEN'
  | 'EMAIL_NOT_VERIFIED'
  | 'CONSENT_REQUIRED'
  | 'NOT_FOUND'
  | 'EMAIL_ALREADY_EXISTS'
  | 'IMAGE_TOO_LARGE'
  | 'IMAGE_UNSUPPORTED'
  | 'IMAGE_UNREADABLE'
  | 'PROBLEM_OUT_OF_SCOPE'
  | 'DAILY_LIMIT_REACHED'
  | 'RATE_LIMITED'
  | 'INTERNAL_ERROR'
  | 'LESSON_GENERATION_FAILED'
  | 'LLM_UNAVAILABLE'
  | 'TTS_UNAVAILABLE';

export interface FieldIssue {
  field: string;
  issue: string;
}

export interface ApiErrorBody {
  error: {
    code: ApiErrorCode;
    message: string;
    details?: FieldIssue[];
    retryAfterSeconds?: number;
    traceId?: string;
  };
}
