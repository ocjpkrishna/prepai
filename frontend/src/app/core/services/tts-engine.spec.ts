import { TestBed } from '@angular/core/testing';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { KokoroTtsEngine, TTS_BASE_URL } from './tts-engine';

describe('KokoroTtsEngine', () => {
	afterEach(() => vi.unstubAllGlobals());

	function engine(): KokoroTtsEngine {
		TestBed.configureTestingModule({
			providers: [KokoroTtsEngine, { provide: TTS_BASE_URL, useValue: 'http://tts.test/tts-api/' }],
		});
		return TestBed.inject(KokoroTtsEngine);
	}

	it('returns the voice list when the service answers', async () => {
		vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ['af_heart', 'bf_emma'] }));
		expect(await engine().voices()).toEqual(['af_heart', 'bf_emma']);
	});

	it('returns null when the service is unreachable', async () => {
		vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new Error('offline')));
		expect(await engine().voices()).toBeNull();
	});

	it('returns null when the host answers 404, as GitHub Pages does', async () => {
		vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 404 }));
		expect(await engine().voices()).toBeNull();
	});

	it('posts the text with the voice and a language that fits it', async () => {
		const fetchMock = vi.fn().mockResolvedValue({ ok: true, blob: async () => new Blob(['x']) });
		vi.stubGlobal('fetch', fetchMock);
		await engine().synthesize('hello', 'bf_emma');
		const [url, init] = fetchMock.mock.calls[0];
		expect(url).toBe('http://tts.test/tts-api/tts');
		expect(JSON.parse(init.body)).toEqual({ text: 'hello', voice: 'bf_emma', speed: 1, lang: 'en-gb' });
	});

	it('throws when synthesis fails', async () => {
		vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: false, status: 500 }));
		await expect(engine().synthesize('hello', 'af_heart')).rejects.toThrow('500');
	});
});
