import { ComponentFixture, TestBed } from '@angular/core/testing';
import { WhiteboardComponent } from './whiteboard.component';
import { SAMPLE_STEPS } from './testing/whiteboard.fixtures';

vi.mock('konva', () => import('./testing/konva-fake'));

describe('WhiteboardComponent', () => {
  let fixture: ComponentFixture<WhiteboardComponent>;

  beforeEach(async () => {
    vi.stubGlobal('requestAnimationFrame', (callback: FrameRequestCallback) =>
      setTimeout(() => callback(performance.now() + 1e6), 0));
    vi.spyOn(console, 'warn').mockImplementation(() => undefined);
    await TestBed.configureTestingModule({ imports: [WhiteboardComponent] }).compileComponents();
    fixture = TestBed.createComponent(WhiteboardComponent);
  });

  afterEach(() => {
    fixture.destroy();
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  it('emits stepComplete once the step has played', async () => {
    const completed: number[] = [];
    fixture.componentInstance.stepComplete.subscribe((stepNumber) => completed.push(stepNumber));
    fixture.componentRef.setInput('step', SAMPLE_STEPS[0]);
    fixture.detectChanges();
    await vi.waitFor(() => expect(completed).toEqual([1]));
  });

  it('shows one panel equation per equation in the step', async () => {
    fixture.componentRef.setInput('step', SAMPLE_STEPS[0]);
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('.wb-equation').length).toBe(2);
  });
});
