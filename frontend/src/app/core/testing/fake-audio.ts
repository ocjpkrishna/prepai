/** A stand-in for HTMLAudioElement: tests drive it by hand. */
export class FakeAudio {
  src = '';
  playbackRate = 1;
  currentTime = 0;
  duration = 10;
  onended: (() => void) | null = null;
  onerror: (() => void) | null = null;
  ontimeupdate: (() => void) | null = null;
  playResult: Promise<void> = Promise.resolve();
  play = vi.fn(() => this.playResult);
  pause = vi.fn();

  asElement(): HTMLAudioElement {
    return this as unknown as HTMLAudioElement;
  }
}
