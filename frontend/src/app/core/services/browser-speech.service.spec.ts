import { TestBed } from '@angular/core/testing';
import { BrowserSpeech, pickFemaleVoice } from './browser-speech.service';

class FakeUtterance {
  lang = '';
  rate = 1;
  pitch = 1;
  voice: unknown = null;
  onend: (() => void) | null = null;
  onerror: ((event: { error: string }) => void) | null = null;
  constructor(readonly text: string) {}
}

const voice = (name: string, lang: string): SpeechSynthesisVoice => ({ name, lang }) as SpeechSynthesisVoice;

describe('pickFemaleVoice', () => {
  it('prefers a natural Indian English female voice over everything else', () => {
    const voices = [
      voice('Microsoft Ravi Online (Natural) - English (India)', 'en-IN'),
      voice('Google UK English Female', 'en-GB'),
      voice('Microsoft Neerja Online (Natural) - English (India)', 'en-IN'),
      voice('Microsoft Zira', 'en-US'),
    ];
    expect(pickFemaleVoice(voices, 'en-IN')?.name).toContain('Neerja');
  });

  it('never picks a male voice, even when it is the only voice of the language', () => {
    expect(pickFemaleVoice([voice('Microsoft David', 'en-US'), voice('Daniel', 'en-GB')], 'en-IN')).toBeNull();
  });

  it('treats a name that says female as female even though it contains male', () => {
    expect(pickFemaleVoice([voice('Google UK English Female', 'en-GB')], 'en-IN')?.name).toBe('Google UK English Female');
  });

  it('ignores voices of another language', () => {
    expect(pickFemaleVoice([voice('Samantha', 'fr-FR')], 'en-IN')).toBeNull();
  });
});

describe('BrowserSpeech', () => {
  let speech: BrowserSpeech;
  let synth: { speak: ReturnType<typeof vi.fn>; cancel: ReturnType<typeof vi.fn>; pause: ReturnType<typeof vi.fn>; resume: ReturnType<typeof vi.fn>; getVoices: ReturnType<typeof vi.fn> };
  let spoken: FakeUtterance[];

  beforeEach(() => {
    spoken = [];
    synth = {
      speak: vi.fn((utterance: FakeUtterance) => spoken.push(utterance)),
      cancel: vi.fn(),
      pause: vi.fn(),
      resume: vi.fn(),
      getVoices: vi.fn(() => [voice('Microsoft David', 'en-US'), voice('Microsoft Neerja Online (Natural)', 'en-IN')]),
    };
    vi.stubGlobal('SpeechSynthesisUtterance', FakeUtterance);
    Object.defineProperty(window, 'speechSynthesis', { value: synth, configurable: true });
    speech = TestBed.inject(BrowserSpeech);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it('is supported when the browser has speech synthesis', () => {
    expect(speech.supported()).toBe(true);
  });

  it('speaks sentence by sentence with the female voice at the rate, and resolves after the last one', async () => {
    const progress = vi.fn();
    const done = speech.speak('Hello there. How are you?', 'en-IN', 1.5, progress);
    await vi.waitFor(() => expect(spoken.length).toBe(2));
    expect(spoken.map((utterance) => utterance.text)).toEqual(['Hello there.', 'How are you?']);
    expect((spoken[0].voice as SpeechSynthesisVoice).name).toContain('Neerja');
    expect(spoken[0].rate).toBe(1.5);
    spoken[0].onend?.();
    expect(progress).toHaveBeenCalled();
    spoken[1].onend?.();
    await expect(done).resolves.toBe(true);
  });

  it('raises the pitch of the default voice when the device has no female voice', async () => {
    synth.getVoices.mockReturnValue([voice('Microsoft David', 'en-US')]);
    void speech.speak('Hi.', 'en-IN', 1, vi.fn());
    await vi.waitFor(() => expect(spoken.length).toBe(1));
    expect(spoken[0].voice).toBeNull();
    expect(spoken[0].pitch).toBeGreaterThan(1);
  });

  it('counts a cancelled voice as finished and any other error as a failure', async () => {
    const cancelled = speech.speak('A.', 'en-IN', 1, vi.fn());
    await vi.waitFor(() => expect(spoken.length).toBe(1));
    spoken[0].onerror?.({ error: 'canceled' });
    await expect(cancelled).resolves.toBe(true);
    const failed = speech.speak('B.', 'en-IN', 1, vi.fn());
    await vi.waitFor(() => expect(spoken.length).toBe(2));
    spoken[1].onerror?.({ error: 'synthesis-failed' });
    await expect(failed).resolves.toBe(false);
  });

  it('passes pause, resume and cancel to the browser', () => {
    speech.pause();
    speech.resume();
    speech.cancel();
    expect(synth.pause).toHaveBeenCalled();
    expect(synth.resume).toHaveBeenCalled();
    expect(synth.cancel).toHaveBeenCalled();
  });
});
