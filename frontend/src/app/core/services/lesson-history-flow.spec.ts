import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { authInterceptor } from '../auth/auth.interceptor';
import { TokenService } from '../auth/token.service';
import { LessonRequest } from '../models/lesson-api.model';
import { LessonService } from './lesson.service';

const GENERATE_URL = '/api/v1/lessons/generate';
const EXTRACT_URL = '/api/v1/lessons/extract';
const HISTORY_URL = '/api/v1/lessons/history';

const QUESTION: LessonRequest = {
  type: 'PROBLEM',
  subject: 'PHYSICS',
  exam: 'JEE_MAIN',
  input: {
    text: 'Find the time of flight of a projectile launched at 60 degrees.',
    imageBase64: null,
  },
  difficulty: 'MEDIUM',
  language: 'en',
};

const LESSON_ID = '11111111-1111-1111-1111-111111111111';

describe('lesson flow: input to history (mocked HTTP)', () => {
  let lessons: LessonService;
  let backend: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting(),
      ],
    });
    TestBed.inject(TokenService).save({ accessToken: 'student-token', refreshToken: 'refresh' });
    lessons = TestBed.inject(LessonService);
    backend = TestBed.inject(HttpTestingController);
  });

  afterEach(() => backend.verify());

  it('posts the typed question with the chosen options and the bearer token', () => {
    let lessonId: string | undefined;
    lessons.generate(QUESTION).subscribe((lesson) => (lessonId = lesson.lessonId));

    const request = backend.expectOne(GENERATE_URL);
    expect(request.request.body).toEqual(QUESTION);
    expect(request.request.headers.get('Authorization')).toBe('Bearer student-token');
    request.flush({ lessonId: LESSON_ID });
    expect(lessonId).toBe(LESSON_ID);
  });

  it('shows the new lesson at the top of the history page', () => {
    let page: { content: { lessonId: string }[] } | undefined;
    lessons.generate(QUESTION).subscribe();
    backend.expectOne(GENERATE_URL).flush({ lessonId: LESSON_ID });

    lessons.history().subscribe((value) => (page = value));
    backend.expectOne(`${HISTORY_URL}?page=0&size=20`).flush({
      content: [
        {
          lessonId: LESSON_ID,
          title: 'Projectile Motion',
          subject: 'PHYSICS',
          createdAt: '2026-10-09T10:00:00Z',
        },
      ],
      page: 0,
      totalPages: 1,
    });
    expect(page?.content[0].lessonId).toBe(LESSON_ID);
  });

  it('filters the history by subject when one is chosen', () => {
    lessons.history(0, 20, 'CHEMISTRY').subscribe();

    const request = backend.expectOne(`${HISTORY_URL}?page=0&size=20&subject=CHEMISTRY`);
    expect(request.request.method).toBe('GET');
    request.flush({ content: [], page: 0, totalPages: 0 });
  });

  it('sends a photo as multipart under the image field and returns the reading', () => {
    let reading: string | undefined;
    lessons
      .extract(new Blob(['jpeg-bytes'], { type: 'image/jpeg' }))
      .subscribe((result) => (reading = result.problemText));

    const request = backend.expectOne(EXTRACT_URL);
    const body = request.request.body as FormData;
    expect(body).toBeInstanceOf(FormData);
    expect(body.get('image')).toBeInstanceOf(Blob);
    request.flush({
      problemText: 'Find the time of flight.',
      confidence: 'HIGH',
      hasDiagram: false,
    });
    expect(reading).toBe('Find the time of flight.');
  });
});
