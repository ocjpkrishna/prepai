import { TestBed } from '@angular/core/testing';
import { provideRouter, Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ApiError } from '../../../core/errors/api-error';
import { LessonService } from '../../../core/services/lesson.service';
import { LessonInputComponent } from './lesson-input.component';

describe('LessonInputComponent', () => {
  let lessons: { generate: ReturnType<typeof vi.fn>; extract: ReturnType<typeof vi.fn> };
  let router: Router;

  beforeEach(() => {
    lessons = {
      generate: vi.fn().mockReturnValue(of({ lessonId: 'lesson-1' })),
      extract: vi.fn(),
    };
    TestBed.configureTestingModule({
      imports: [LessonInputComponent],
      providers: [provideRouter([]), { provide: LessonService, useValue: lessons }],
    });
    router = TestBed.inject(Router);
    vi.spyOn(router, 'navigate').mockResolvedValue(true);
  });

  function create() {
    const fixture = TestBed.createComponent(LessonInputComponent);
    fixture.detectChanges();
    return fixture;
  }

  it('submits a typed problem and opens the new lesson', () => {
    const fixture = create();
    fixture.componentInstance['form'].patchValue({ text: 'Find the range of a projectile' });

    fixture.componentInstance['submitText']();
    expect(lessons.generate).toHaveBeenCalledWith(expect.objectContaining({
      type: 'PROBLEM',
      input: { text: 'Find the range of a projectile', imageBase64: null },
    }));
    expect(router.navigate).toHaveBeenCalledWith(['/lessons', 'lesson-1']);
  });

  it('sends a blank question nowhere', () => {
    const fixture = create();
    fixture.componentInstance['submitText']();
    expect(lessons.generate).not.toHaveBeenCalled();
  });

  it('redirects to guardian consent when the lesson is blocked', () => {
    lessons.generate.mockReturnValue(throwError(() => new ApiError({ status: 403, code: 'CONSENT_REQUIRED', message: '' })));
    const fixture = create();
    fixture.componentInstance['form'].patchValue({ text: 'Question' });

    fixture.componentInstance['submitText']();
    expect(router.navigate).toHaveBeenCalledWith(['/guardian-pending']);
  });

  it('rejects an unsupported photo type on the client, before any upload', async () => {
    const fixture = create();
    const file = new File(['gif'], 'problem.gif', { type: 'image/gif' });

    await fixture.componentInstance['onPhotoSelected']({ target: { files: [file] } } as unknown as Event);
    expect(lessons.extract).not.toHaveBeenCalled();
    expect(fixture.componentInstance['error']()?.code).toBe('IMAGE_UNSUPPORTED');
  });

  it('keeps the student on the page and shows the friendly message when generation fails', () => {
    lessons.generate.mockReturnValue(throwError(() => new ApiError({ status: 503, code: 'LLM_UNAVAILABLE', message: '' })));
    const fixture = create();
    fixture.componentInstance['form'].patchValue({ text: 'Question' });

    fixture.componentInstance['submitText']();
    expect(router.navigate).not.toHaveBeenCalled();
    expect(fixture.componentInstance['error']()?.code).toBe('LLM_UNAVAILABLE');
  });
});
