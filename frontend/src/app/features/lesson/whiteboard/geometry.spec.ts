import { clampToCanvas, polar, quadraticPoint, stagger } from './geometry';
import { easeInOut, easeOutCubic, linear } from './easing';

describe('geometry and easing', () => {
  it('measures angles counter-clockwise on a screen whose y points down', () => {
    const up = polar({ x: 100, y: 300 }, 50, 90);
    expect(up.x).toBeCloseTo(100);
    expect(up.y).toBeCloseTo(250);
  });

  it('passes a parabola through its peak at the midpoint of the curve', () => {
    const control = { x: 275, y: -100 };
    const apex = quadraticPoint({ x: 100, y: 300 }, control, { x: 450, y: 300 }, 0.5);
    expect(apex.x).toBeCloseTo(275);
    expect(apex.y).toBeCloseTo(100);
  });

  it('staggers items one after another', () => {
    expect(stagger(0.5, 0, 2)).toBe(1);
    expect(stagger(0.5, 1, 2)).toBe(0);
    expect(stagger(0.75, 1, 2)).toBeCloseTo(0.5);
  });

  it('clamps points into the 800 by 500 canvas', () => {
    expect(clampToCanvas({ x: -1, y: 600 })).toEqual({ x: 0, y: 500 });
  });

  it.each([linear, easeOutCubic, easeInOut])('starts at 0 and ends at 1 (easing %#)', (ease) => {
    expect(ease(0)).toBe(0);
    expect(ease(1)).toBe(1);
  });

  it('eases out faster than linear, and easeInOut is symmetric', () => {
    expect(easeOutCubic(0.5)).toBeGreaterThan(0.5);
    expect(easeInOut(0.5)).toBeCloseTo(0.5);
  });
});
