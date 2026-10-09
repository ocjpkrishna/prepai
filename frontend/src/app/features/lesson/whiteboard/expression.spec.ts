import { ActionConfigError } from './action-config';
import { compileExpression } from './expression';

describe('compileExpression', () => {
  it('evaluates sums, products and powers', () => {
    expect(compileExpression('x^2 - 3x + 2')(2)).toBe(0);
  });

  it('supports functions and constants', () => {
    expect(compileExpression('sin(x)')(Math.PI / 2)).toBeCloseTo(1);
    expect(compileExpression('log(100)')(0)).toBeCloseTo(2);
    expect(compileExpression('2pi')(0)).toBeCloseTo(2 * Math.PI);
  });

  it('gives a unary minus lower precedence than a power', () => {
    expect(compileExpression('-x^2')(2)).toBe(-4);
  });

  it('accepts implicit multiplication and parentheses', () => {
    expect(compileExpression('2(x+1)')(2)).toBe(6);
  });

  it('rejects anything that is not a plain expression', () => {
    for (const source of ['', 'x +', 'foo(x)', 'x; alert(1)', 'sin x', 'x)(']) {
      expect(() => compileExpression(source)).toThrow(ActionConfigError);
    }
  });
});
