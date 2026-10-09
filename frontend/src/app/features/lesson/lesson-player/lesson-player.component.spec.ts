import { signal } from '@angular/core';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ApiError } from '../../../core/errors/api-error';
import { MOCK_FULL_LESSON } from '../../../core/mock/mock-lesson';
import { LessonService } from '../../../core/services/lesson.service';
import { TtsService } from '../../../core/services/tts.service';
import { LessonPlayerComponent } from './lesson-player.component';

vi.mock('konva', () => import('../whiteboard/testing/konva-fake'));

describe('LessonPlayerComponent', () => {
  let fixture: ComponentFixture<LessonPlayerComponent>;
  let get: ReturnType<typeof vi.fn>;
  const available = signal(true);

  beforeEach(async () => {
    available.set(true);
    get = vi.fn(() => of(MOCK_FULL_LESSON));
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) =>
      setTimeout(() => callback(performance.now() + 1e6), 0));
    vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    await TestBed.configureTestingModule({
      imports: [LessonPlayerComponent],
      providers: [
        provideRouter([]),
        { provide: LessonService, useValue: { get } },
        {
          provide: TtsService,
          useValue: {
            available,
            prefetch: vi.fn(),
            speak: vi.fn(async () => true),
            pause: vi.fn(),
            resume: vi.fn(),
            stop: vi.fn(),
            setSpeed: vi.fn(),
            retry: vi.fn(),
          },
        },
      ],
    }).compileComponents();
    fixture = TestBed.createComponent(LessonPlayerComponent);
    fixture.componentRef.setInput('lessonId', MOCK_FULL_LESSON.lessonId);
  });

  afterEach(() => {
    fixture.destroy();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  const text = (): string => fixture.nativeElement.textContent;

  it('loads the lesson and waits for the Start button', () => {
    fixture.detectChanges();
    expect(get).toHaveBeenCalledWith(MOCK_FULL_LESSON.lessonId);
    expect(text()).toContain(MOCK_FULL_LESSON.title);
    expect(text()).toContain('Start lesson');
  });

  it('shows captions and controls after Start, and plays on to the summary', async () => {
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button').click();
    fixture.detectChanges();
    expect(text()).toContain('Step 1 of 3');
    expect(fixture.nativeElement.querySelector('[data-testid="caption"]').textContent).toContain('First, we break');
    await vi.waitFor(() => {
      fixture.detectChanges();
      expect(text()).toContain('Replay lesson');
    }, { timeout: 4000 });
    expect(text()).toContain('Range: 20√3 m');
  });

  it('offers Retry voice when the voice is unavailable', () => {
    fixture.detectChanges();
    fixture.nativeElement.querySelector('button').click();
    available.set(false);
    fixture.detectChanges();
    expect(text()).toContain('Retry voice');
  });

  it('shows the error when the lesson cannot be loaded', () => {
    get.mockReturnValue(throwError(() => new ApiError({ status: 404, code: 'NOT_FOUND', message: 'x' })));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('app-api-error')).not.toBeNull();
  });
});
