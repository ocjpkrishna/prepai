import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { map, Observable, tap } from 'rxjs';
import { environment } from '../../../environments/environment';
import { AuthTokens, RegisterRequest } from '../models/user.model';
import { TokenService } from './token.service';

const AUTH_PATH = `${environment.apiBaseUrl}/auth`;

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly tokens = inject(TokenService);
  private readonly router = inject(Router);

  isLoggedIn(): boolean {
    return this.tokens.accessToken() !== null;
  }

  register(request: RegisterRequest): Observable<void> {
    return this.http.post<void>(`${AUTH_PATH}/register`, request);
  }

  login(email: string, password: string): Observable<AuthTokens> {
    return this.http.post<AuthTokens>(`${AUTH_PATH}/login`, { email, password }).pipe(tap(tokens => this.tokens.save(tokens)));
  }

  loginWithGoogle(idToken: string): Observable<AuthTokens> {
    return this.http.post<AuthTokens>(`${AUTH_PATH}/google`, { idToken }).pipe(tap(tokens => this.tokens.save(tokens)));
  }

  refresh(): Observable<AuthTokens> {
    const refreshToken = this.tokens.refreshToken();
    return this.http.post<AuthTokens>(`${AUTH_PATH}/refresh`, { refreshToken }).pipe(tap(tokens => this.tokens.save(tokens)));
  }

  verifyEmail(token: string): Observable<void> {
    return this.http.get<void>(`${AUTH_PATH}/verify-email`, { params: { token } });
  }

  confirmGuardianConsent(token: string): Observable<void> {
    return this.http.post<void>(`${AUTH_PATH}/guardian-consent/confirm`, null, { params: { token } });
  }

  /** A deliberate logout (or a deleted account) goes to the landing page, not back to a protected page. */
  logout(): void {
    this.tokens.clear();
    void this.router.navigate(['/']);
  }

  /** Clears the tokens and sends the student to log in, remembering where they were. */
  endSession(returnUrl: string = this.router.url): void {
    this.tokens.clear();
    void this.router.navigate(['/login'], { queryParams: { returnUrl } });
  }

  accessToken(): string | null {
    return this.tokens.accessToken();
  }

  /** Lets callers chain a refresh and then read the new access token. */
  refreshedAccessToken(): Observable<string | null> {
    return this.refresh().pipe(map(() => this.tokens.accessToken()));
  }
}
