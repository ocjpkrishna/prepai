const DEFAULT_LANDING_PAGE = '/dashboard';

/** Only same-site paths are followed after login, so a link cannot send a student to another site. */
export function safeReturnUrl(returnUrl: string | undefined): string {
  if (!returnUrl || !returnUrl.startsWith('/') || returnUrl.startsWith('//')) {
    return DEFAULT_LANDING_PAGE;
  }
  return returnUrl;
}
