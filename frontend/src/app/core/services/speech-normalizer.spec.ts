import { describe, expect, it } from 'vitest';
import { normalizeForSpeech } from './speech-normalizer';

describe('normalizeForSpeech', () => {
	it('speaks the problem statement that Kokoro misread', () => {
		const text = 'A ball is thrown horizontally with speed 10 m/s from the top of a building 80 m tall. Find the time to reach the ground and the distance from the base of the building where it lands. (g = 10 m/s²)';
		expect(normalizeForSpeech(text)).toBe(
			'A ball is thrown horizontally with speed 10 metres per second from the top of a building 80 metres tall. Find the time to reach the ground and the distance from the base of the building where it lands. (g equals 10 metres per second squared)',
		);
	});

	it('speaks common physics units', () => {
		expect(normalizeForSpeech('F = 5 N and v = 36 km/h')).toBe('F equals 5 newtons and v equals 36 kilometres per hour');
		expect(normalizeForSpeech('density 1000 kg/m³')).toBe('density 1000 kilograms per metre cubed');
		expect(normalizeForSpeech('a = 2 m/s^2')).toBe('a equals 2 metres per second squared');
	});

	it('uses the singular for exactly one', () => {
		expect(normalizeForSpeech('after 1 s and 1 m')).toBe('after 1 second and 1 metre');
	});

	it('does not touch variables that look like units', () => {
		expect(normalizeForSpeech('the mass m and time s')).toBe('the mass m and time s');
	});

	it('speaks operators, powers and subscripts', () => {
		expect(normalizeForSpeech('80 = 10t')).toBe('80 equals 10 t');
		expect(normalizeForSpeech('t^2 = 16')).toBe('t squared equals 16');
		expect(normalizeForSpeech('u_y t + 1/2 g t²')).toBe('u sub y t plus 1 over 2 g t squared');
		expect(normalizeForSpeech('10 × 4 = 40')).toBe('10 times 4 equals 40');
		expect(normalizeForSpeech('sin 60° = √3/2')).toBe('sin 60 degrees equals square root of 3 over 2');
	});

	it('speaks degrees Celsius and percent', () => {
		expect(normalizeForSpeech('25 °C and 40%')).toBe('25 degrees Celsius and 40 percent');
	});
});
