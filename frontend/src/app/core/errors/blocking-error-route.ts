import { ApiError } from './api-error';

/** Where a student is sent when an error means "not yet allowed", or null when the page shows the error itself. */
export function blockingErrorRoute(error: ApiError): string | null {
  switch (error.code) {
    case 'EMAIL_NOT_VERIFIED':
      return '/verify-email';
    case 'CONSENT_REQUIRED':
      return '/guardian-pending';
    default:
      return null;
  }
}
