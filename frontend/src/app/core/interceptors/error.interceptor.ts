import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { toApiError } from '../errors/api-error';

/** Turns every HTTP failure into an ApiError, so pages handle one shape (spec 4.7). */
export const errorInterceptor: HttpInterceptorFn = (req, next) =>
  next(req).pipe(catchError((error: unknown) => throwError(() => asApiError(error))));

function asApiError(error: unknown): unknown {
  return error instanceof HttpErrorResponse ? toApiError(error) : error;
}
