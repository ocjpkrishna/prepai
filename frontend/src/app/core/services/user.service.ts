import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { User, Usage } from '../models/user.model';

const USER_PATH = `${environment.apiBaseUrl}/users`;

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  me(): Observable<User> {
    return this.http.get<User>(`${USER_PATH}/me`);
  }

  usage(): Observable<Usage> {
    return this.http.get<Usage>(`${USER_PATH}/me/usage`);
  }

  savePreferences(language: 'en' | 'hi'): Observable<void> {
    return this.http.put<void>(`${USER_PATH}/me/preferences`, { language, voiceSpeed: 1.0, theme: 'dark' });
  }

  /** The export is a JSON file the student saves, so it is read as a Blob. */
  exportData(): Observable<Blob> {
    return this.http.get(`${USER_PATH}/me/export`, { responseType: 'blob' });
  }

  deleteAccount(): Observable<void> {
    return this.http.delete<void>(`${USER_PATH}/me`);
  }
}
