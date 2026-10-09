import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ExtractResult, LessonHistoryPage, LessonRequest, LessonResponse, Subject } from '../models/lesson.model';

const LESSON_PATH = `${environment.apiBaseUrl}/lessons`;
const IMAGE_FIELD = 'image';
const IMAGE_FILE_NAME = 'problem.jpg';

@Injectable({ providedIn: 'root' })
export class LessonService {
  private readonly http = inject(HttpClient);

  generate(request: LessonRequest): Observable<LessonResponse> {
    return this.http.post<LessonResponse>(`${LESSON_PATH}/generate`, request);
  }

  extract(image: Blob): Observable<ExtractResult> {
    const form = new FormData();
    form.append(IMAGE_FIELD, image, IMAGE_FILE_NAME);
    return this.http.post<ExtractResult>(`${LESSON_PATH}/extract`, form);
  }

  history(page = 0, size = 20, subject?: Subject): Observable<LessonHistoryPage> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (subject) {
      params = params.set('subject', subject);
    }
    return this.http.get<LessonHistoryPage>(`${LESSON_PATH}/history`, { params });
  }
}
