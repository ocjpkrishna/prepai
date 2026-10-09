import { isMinor } from './age-gate';
import { safeReturnUrl } from './return-url';

describe('isMinor', () => {
  const today = new Date('2026-10-09T00:00:00');

  it('is true until the 18th birthday', () => {
    expect(isMinor('2008-10-10', today)).toBe(true);
  });

  it('is false on the 18th birthday', () => {
    expect(isMinor('2008-10-09', today)).toBe(false);
  });

  it('treats an empty or invalid date as not a minor, so the backend decides', () => {
    expect(isMinor('', today)).toBe(false);
  });
});

describe('safeReturnUrl', () => {
  it('follows same-site paths only', () => {
    expect(safeReturnUrl('/profile')).toBe('/profile');
    expect(safeReturnUrl('https://evil.example')).toBe('/dashboard');
    expect(safeReturnUrl('//evil.example')).toBe('/dashboard');
    expect(safeReturnUrl(undefined)).toBe('/dashboard');
  });
});
