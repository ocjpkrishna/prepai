import { HttpContextToken, HttpErrorResponse, HttpEvent, HttpHandlerFn, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, Observable, switchMap, throwError } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthService } from './auth.service';

const UNAUTHORIZED = 401;
const AUTH_PATH = `${environment.apiBaseUrl}/auth`;

/** Set on the one retry after a silent refresh, so a second 401 is not refreshed again. */
const RETRIED_AFTER_REFRESH = new HttpContextToken<boolean>(() => false);

/** Adds the bearer token and, on one 401, refreshes the session and retries the request once. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const authed = withBearer(req, auth.accessToken());
  return next(authed).pipe(catchError(error => recoverFromUnauthorized(error, req, next, auth)));
};

function recoverFromUnauthorized(
  error: unknown,
  req: HttpRequest<unknown>,
  next: HttpHandlerFn,
  auth: AuthService,
): Observable<HttpEvent<unknown>> {
  if (!shouldRefresh(error, req)) {
    return throwError(() => error);
  }
  return auth.refreshedAccessToken().pipe(
    switchMap(token => next(withBearer(retryContext(req), token))),
    catchError(() => {
      auth.endSession();
      return throwError(() => error);
    }),
  );
}

function shouldRefresh(error: unknown, req: HttpRequest<unknown>): boolean {
  return isUnauthorized(error) && !isAuthCall(req) && !req.context.get(RETRIED_AFTER_REFRESH);
}

function isUnauthorized(error: unknown): boolean {
  return error instanceof HttpErrorResponse && error.status === UNAUTHORIZED;
}

function isAuthCall(req: HttpRequest<unknown>): boolean {
  return req.url.startsWith(AUTH_PATH);
}

function retryContext(req: HttpRequest<unknown>): HttpRequest<unknown> {
  return req.clone({ context: req.context.set(RETRIED_AFTER_REFRESH, true) });
}

function withBearer(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  if (!token || isAuthCall(req)) {
    return req;
  }
  return req.clone({ setHeaders: { Authorization: `Bearer ${token}` } });
}
