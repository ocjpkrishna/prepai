import { mockSynthesis } from './mock-audio';

describe('mockSynthesis', () => {
  it('returns a WAV data URL whose length follows the text', () => {
    const short = mockSynthesis('Hello there');
    const long = mockSynthesis('one two three four five six seven eight nine ten');
    expect(short.audioUrl.startsWith('data:audio/wav;base64,UklGR')).toBe(true);
    expect(long.durationMs).toBeGreaterThan(short.durationMs);
  });
});
