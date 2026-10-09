import { HttpInterceptorFn, HttpRequest, HttpResponse } from '@angular/common/http';
import { delay, Observable, of } from 'rxjs';
import { environment } from '../../../environments/environment';
import { MOCK_ROUTES, MockRoute } from './mock-routes';

const MOCK_LATENCY_MS = 300;

/** Answers API calls from fixtures while the backend is not reachable (environment.useMockBackend). */
export const mockBackendInterceptor: HttpInterceptorFn = (req, next) => {
  const route = environment.useMockBackend ? findRoute(req) : undefined;
  return route ? respond(route, req) : next(req);
};

function findRoute(req: HttpRequest<unknown>): MockRoute | undefined {
  const path = req.url.replace(environment.apiBaseUrl, '').split('?')[0];
  return MOCK_ROUTES.find(route => route.method === req.method && pathMatches(route.path, path));
}

/** A route segment that starts with ':' matches any single segment. */
function pathMatches(pattern: string, path: string): boolean {
  const wanted = pattern.split('/');
  const actual = path.split('/');
  return wanted.length === actual.length && wanted.every((part, i) => part.startsWith(':') || part === actual[i]);
}

function respond(route: MockRoute, req: HttpRequest<unknown>): Observable<HttpResponse<unknown>> {
  const response = new HttpResponse({ status: route.status ?? 200, body: route.body(req) });
  return of(response).pipe(delay(MOCK_LATENCY_MS));
}
