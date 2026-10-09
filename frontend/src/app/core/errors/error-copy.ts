import { ErrorCode } from './api-error';

/** What the student sees for each error code (spec 4.7). Keys are exhaustive, so a new code will not compile without copy. */
export const ERROR_COPY: Record<ErrorCode, string> = {
  VALIDATION_FAILED: 'Please check the highlighted fields.',
  UNAUTHENTICATED: 'Your session has ended. Please log in again.',
  FORBIDDEN: "You don't have access to this.",
  EMAIL_NOT_VERIFIED: 'Please verify your email before starting a lesson.',
  CONSENT_REQUIRED: 'Waiting for guardian consent before lessons can start.',
  NOT_FOUND: 'We could not find that page.',
  EMAIL_ALREADY_EXISTS: 'An account with this email already exists.',
  IMAGE_TOO_LARGE: 'Image too large, try again with a smaller photo.',
  IMAGE_UNSUPPORTED: 'Use a JPEG, PNG or WebP photo.',
  IMAGE_UNREADABLE: "We couldn't read this photo. Please retake it in better light, closer to the page.",
  PROBLEM_OUT_OF_SCOPE: 'PrepAI covers Physics, Chemistry and Maths.',
  DAILY_LIMIT_REACHED: "You've used all your free sessions today.",
  RATE_LIMITED: 'Slow down. You can try again in a moment.',
  INTERNAL_ERROR: 'Something went wrong on our side. Please try again.',
  LESSON_GENERATION_FAILED: "Couldn't build this lesson. No session was used.",
  LLM_UNAVAILABLE: 'Tutor is busy, try again shortly.',
  TTS_UNAVAILABLE: 'Voice is unavailable right now. Captions will show instead.',
  NETWORK_UNAVAILABLE: 'You appear to be offline. Check your connection and try again.',
};

/** Transient codes where a Retry button makes sense. */
export const RETRYABLE_CODES: readonly ErrorCode[] = ['LESSON_GENERATION_FAILED', 'LLM_UNAVAILABLE', 'NETWORK_UNAVAILABLE', 'INTERNAL_ERROR'];
