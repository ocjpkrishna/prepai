import { SynthesizeResult } from '../models/tts.model';

const SAMPLE_RATE = 8000;
const HEADER_BYTES = 44;
const MS_PER_WORD = 350;
const MIN_DURATION_MS = 800;
const SILENCE_BYTE = 0x80;
const MS_PER_SECOND = 1000;

/** A silent 8-bit mono WAV as a data URL, sized to how long the text would take to read aloud. */
export function mockSynthesis(text: string): SynthesizeResult {
  const words = text.split(/\s+/).filter(Boolean).length;
  const durationMs = Math.max(MIN_DURATION_MS, words * MS_PER_WORD);
  return { audioUrl: `data:audio/wav;base64,${silentWavBase64(durationMs)}`, durationMs };
}

function silentWavBase64(durationMs: number): string {
  const samples = Math.round((SAMPLE_RATE * durationMs) / MS_PER_SECOND);
  const bytes = new Uint8Array(HEADER_BYTES + samples).fill(SILENCE_BYTE, HEADER_BYTES);
  const view = new DataView(bytes.buffer);
  writeText(bytes, 0, 'RIFF');
  view.setUint32(4, HEADER_BYTES - 8 + samples, true);
  writeText(bytes, 8, 'WAVEfmt ');
  view.setUint32(16, 16, true);
  view.setUint16(20, 1, true);
  view.setUint16(22, 1, true);
  view.setUint32(24, SAMPLE_RATE, true);
  view.setUint32(28, SAMPLE_RATE, true);
  view.setUint16(32, 1, true);
  view.setUint16(34, 8, true);
  writeText(bytes, 36, 'data');
  view.setUint32(40, samples, true);
  return encode(bytes);
}

function encode(bytes: Uint8Array): string {
  const chunk = 0x8000;
  let binary = '';
  for (let offset = 0; offset < bytes.length; offset += chunk) {
    binary += String.fromCharCode(...bytes.subarray(offset, offset + chunk));
  }
  return btoa(binary);
}

function writeText(bytes: Uint8Array, offset: number, text: string): void {
  [...text].forEach((char, index) => (bytes[offset + index] = char.charCodeAt(0)));
}
