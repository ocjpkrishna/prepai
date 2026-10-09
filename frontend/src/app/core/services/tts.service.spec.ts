import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { TestBed } from '@angular/core/testing';
import { FakeAudio } from '../testing/fake-audio';
import { AUDIO_FACTORY, pickPreferredVoice, TtsService } from './tts.service';

describe('TtsService', () => {
  let tts: TtsService;
  let http: HttpTestingController;
  let audio: FakeAudio;

  beforeEach(() => {
    audio = new FakeAudio();
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: AUDIO_FACTORY, useValue: () => audio.asElement() },
      ],
    });
    tts = TestBed.inject(TtsService);
    http = TestBed.inject(HttpTestingController);
  });

  const answer = (): void => {
    http.expectOne('/api/v1/tts/synthesize').flush({ audioUrl: 'blob:a', durationMs: 1000 });
  };

  it('plays the audio at the chosen speed and resolves when it ends', async () => {
    tts.setSpeed(1.5);
    const progress = vi.fn();
    const done = tts.speak('Hello', 'en-IN', progress);
    answer();
    await vi.waitFor(() => expect(audio.play).toHaveBeenCalled());
    expect(audio.src).toBe('blob:a');
    expect(audio.playbackRate).toBe(1.5);
    audio.currentTime = 5;
    audio.ontimeupdate?.();
    expect(progress).toHaveBeenLastCalledWith(0.5);
    audio.onended?.();
    await expect(done).resolves.toBe(true);
  });

  it('sends the text and language, and asks the server once for a prefetched text', async () => {
    tts.prefetch('Next', 'en-IN');
    const request = http.expectOne('/api/v1/tts/synthesize');
    expect(request.request.body).toEqual({ text: 'Next', language: 'en-IN' });
    request.flush({ audioUrl: 'blob:n', durationMs: 500 });
    const done = tts.speak('Next', 'en-IN', vi.fn());
    await vi.waitFor(() => expect(audio.play).toHaveBeenCalled());
    http.expectNone('/api/v1/tts/synthesize');
    audio.onended?.();
    await expect(done).resolves.toBe(true);
  });

  it('resolves false and turns unavailable when the endpoint fails', async () => {
    const done = tts.speak('Hello', 'en-IN', vi.fn());
    http.expectOne('/api/v1/tts/synthesize').flush({ code: 'TTS_UNAVAILABLE' }, { status: 503, statusText: 'x' });
    await expect(done).resolves.toBe(false);
    expect(tts.available()).toBe(false);
    tts.retry();
    expect(tts.available()).toBe(true);
  });

  it('resolves false when the audio errors', async () => {
    const done = tts.speak('Hello', 'en-IN', vi.fn());
    answer();
    await vi.waitFor(() => expect(audio.play).toHaveBeenCalled());
    audio.onerror?.();
    await expect(done).resolves.toBe(false);
    expect(tts.available()).toBe(false);
  });

  it('resolves false when the browser blocks playback', async () => {
    audio.playResult = Promise.reject(new Error('NotAllowedError'));
    const done = tts.speak('Hello', 'en-IN', vi.fn());
    answer();
    await expect(done).resolves.toBe(false);
  });

  it('pauses and resumes the audio, and stop ends the narration without failing', async () => {
    const done = tts.speak('Hello', 'en-IN', vi.fn());
    answer();
    await vi.waitFor(() => expect(audio.play).toHaveBeenCalledTimes(1));
    tts.pause();
    expect(audio.pause).toHaveBeenCalled();
    tts.resume();
    expect(audio.play).toHaveBeenCalledTimes(2);
    tts.stop();
    await expect(done).resolves.toBe(true);
    expect(tts.available()).toBe(true);
  });

  it('does not start playing when paused before the audio arrives', async () => {
    const done = tts.speak('Hello', 'en-IN', vi.fn());
    tts.pause();
    answer();
    await vi.waitFor(() => expect(audio.src).toBe('blob:a'));
    expect(audio.play).not.toHaveBeenCalled();
    tts.resume();
    expect(audio.play).toHaveBeenCalledOnce();
    audio.onended?.();
    await expect(done).resolves.toBe(true);
  });

  it('prefers Indian English voices', () => {
    const voices = [
      { id: 'a', name: 'A', language: 'hi-IN' },
      { id: 'b', name: 'B', language: 'en-US' },
      { id: 'c', name: 'C', language: 'en-IN' },
    ];
    expect(pickPreferredVoice(voices)?.id).toBe('c');
    expect(pickPreferredVoice(voices.slice(0, 2))?.id).toBe('b');
    expect(pickPreferredVoice([])).toBeNull();
  });
});
