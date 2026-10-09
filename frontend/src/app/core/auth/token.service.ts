import { Injectable } from '@angular/core';
import { AuthTokens } from '../models/user.model';

const ACCESS_KEY = 'prepai.accessToken';
const REFRESH_KEY = 'prepai.refreshToken';

/** Keeps the JWT pair in localStorage. Storage can be blocked (private mode), so every access is guarded. */
@Injectable({ providedIn: 'root' })
export class TokenService {
  accessToken(): string | null {
    return this.read(ACCESS_KEY);
  }

  refreshToken(): string | null {
    return this.read(REFRESH_KEY);
  }

  save(tokens: AuthTokens): void {
    this.write(ACCESS_KEY, tokens.accessToken);
    this.write(REFRESH_KEY, tokens.refreshToken);
  }

  clear(): void {
    this.remove(ACCESS_KEY);
    this.remove(REFRESH_KEY);
  }

  private read(key: string): string | null {
    try {
      return localStorage.getItem(key);
    } catch {
      return null;
    }
  }

  private write(key: string, value: string): void {
    try {
      localStorage.setItem(key, value);
    } catch {
      // Storage is unavailable; the student will be asked to log in again.
    }
  }

  private remove(key: string): void {
    try {
      localStorage.removeItem(key);
    } catch {
      // Nothing to remove when storage is unavailable.
    }
  }
}
