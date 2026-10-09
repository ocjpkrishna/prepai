import { ActionConfig, ActionConfigError } from './action-config';

describe('ActionConfig', () => {
  it('reads valid fields without notes', () => {
    const config = new ActionConfig({ radius: 5, color: '#4A90D9', origin: { x: 10, y: 20 } });
    expect(config.number('radius')).toBe(5);
    expect(config.color('color', '#000000')).toBe('#4A90D9');
    expect(config.point('origin')).toEqual({ x: 10, y: 20 });
    expect(config.notes).toEqual([]);
  });

  it('takes the fallback for an invalid field and notes it', () => {
    const config = new ActionConfig({ fontSize: 'big', color: 'red;}' });
    expect(config.number('fontSize', { fallback: 16 })).toBe(16);
    expect(config.color('color', '#F3F4F6')).toBe('#F3F4F6');
    expect(config.notes).toEqual(['fontSize is invalid, default used', 'color is invalid, default used']);
  });

  it('takes the fallback for a missing field without a note', () => {
    const config = new ActionConfig({});
    expect(config.number('missing', { fallback: 1 })).toBe(1);
    expect(config.notes).toEqual([]);
  });

  it('throws when a field without a fallback is missing or not finite', () => {
    const config = new ActionConfig({ xLength: NaN });
    expect(() => config.number('xLength')).toThrow(ActionConfigError);
    expect(() => config.point('origin')).toThrow(ActionConfigError);
  });

  it('clamps points to the canvas and numbers to their range, with a note for each', () => {
    const config = new ActionConfig({ at: { x: 900, y: -50 }, radius: -20 });
    expect(config.point('at')).toEqual({ x: 800, y: 0 });
    expect(config.number('radius', { min: 0 })).toBe(0);
    expect(config.notes).toEqual(['at is outside the canvas, clamped', 'radius is outside its range, clamped']);
  });

  it('drops invalid list entries and keeps the valid ones', () => {
    const config = new ActionConfig({ points: [{ x: 1, y: 2 }, { x: 'a', y: 1 }, 'nope', { x: 3, y: 4 }] });
    expect(config.points('points', 2)).toEqual([{ x: 1, y: 2 }, { x: 3, y: 4 }]);
    expect(config.notes).toEqual(['points has 2 invalid entries, skipped']);
  });

  it('throws when a list has fewer valid entries than it needs', () => {
    const config = new ActionConfig({ points: [{ x: 1, y: 1 }] });
    expect(() => config.points('points', 3)).toThrow(ActionConfigError);
  });

  it('reads a range and falls back on a backwards one', () => {
    expect(new ActionConfig({ xRange: [-2, 2] }).range('xRange', [0, 1])).toEqual([-2, 2]);
    const backwards = new ActionConfig({ xRange: [5, -5] });
    expect(backwards.range('xRange', [0, 1])).toEqual([0, 1]);
    expect(backwards.notes).toEqual(['xRange is invalid, default used']);
  });

  it('gives each nested item its own reader', () => {
    const forces = new ActionConfig({ forces: [{ magnitude: 3 }, 'x'] }).list('forces');
    expect(forces).toHaveLength(1);
    expect(forces[0].number('magnitude')).toBe(3);
  });
});
