import { ActionConfigError } from './action-config';

export type ExpressionFn = (x: number) => number;

const MAX_LENGTH = 200;
const TOKEN_PATTERN = /\d+(?:\.\d+)?|[a-z]+|[-+*/^()]/g;
const INVALID_MESSAGE = 'fn is not a valid expression';

const FUNCTIONS: Readonly<Record<string, (value: number) => number>> = {
  sin: Math.sin,
  cos: Math.cos,
  tan: Math.tan,
  sqrt: Math.sqrt,
  abs: Math.abs,
  exp: Math.exp,
  ln: Math.log,
  log: Math.log10,
};

const CONSTANTS: Readonly<Record<string, number>> = { pi: Math.PI, e: Math.E };

/**
 * Compiles a function such as `x^2 - 3x + sin(x)` into a plain function of x.
 * The text is parsed by hand, never evaluated, so a graph from the server cannot run code.
 */
export function compileExpression(source: string): ExpressionFn {
  const compact = source.toLowerCase().replace(/\s+/g, '');
  const tokens = compact.match(TOKEN_PATTERN) ?? [];
  if (compact.length === 0 || compact.length > MAX_LENGTH || tokens.join('') !== compact) {
    throw new ActionConfigError(INVALID_MESSAGE);
  }
  return new ExpressionParser(tokens).parse();
}

/** Recursive descent: sum, product (with implicit multiplication), unary, power, primary. */
class ExpressionParser {
  private position = 0;

  constructor(private readonly tokens: string[]) {}

  parse(): ExpressionFn {
    const expression = this.sum();
    if (this.position < this.tokens.length) {
      throw this.invalid();
    }
    return expression;
  }

  private sum(): ExpressionFn {
    let left = this.product();
    while (this.peekIs('+', '-')) {
      const operator = this.take();
      const right = this.product();
      const previous = left;
      left = operator === '+' ? (x) => previous(x) + right(x) : (x) => previous(x) - right(x);
    }
    return left;
  }

  private product(): ExpressionFn {
    let left = this.unary();
    while (this.peekIs('*', '/') || this.startsOperand()) {
      const operator = this.peekIs('*', '/') ? this.take() : '*';
      const right = this.unary();
      const previous = left;
      left = operator === '/' ? (x) => previous(x) / right(x) : (x) => previous(x) * right(x);
    }
    return left;
  }

  private unary(): ExpressionFn {
    if (this.peekIs('-')) {
      this.take();
      const operand = this.unary();
      return (x) => -operand(x);
    }
    if (this.peekIs('+')) {
      this.take();
      return this.unary();
    }
    return this.power();
  }

  private power(): ExpressionFn {
    const base = this.primary();
    if (!this.peekIs('^')) {
      return base;
    }
    this.take();
    const exponent = this.unary();
    return (x) => base(x) ** exponent(x);
  }

  private primary(): ExpressionFn {
    const token = this.take();
    if (token === undefined) {
      throw this.invalid();
    }
    if (token === '(') {
      const inner = this.sum();
      this.expect(')');
      return inner;
    }
    if (/^\d/.test(token)) {
      const value = Number(token);
      return () => value;
    }
    if (token === 'x') {
      return (x) => x;
    }
    return this.named(token);
  }

  private named(name: string): ExpressionFn {
    if (Object.hasOwn(CONSTANTS, name)) {
      const value = CONSTANTS[name];
      return () => value;
    }
    if (Object.hasOwn(FUNCTIONS, name)) {
      const fn = FUNCTIONS[name];
      this.expect('(');
      const argument = this.sum();
      this.expect(')');
      return (x) => fn(argument(x));
    }
    throw this.invalid();
  }

  private take(): string | undefined {
    const token = this.tokens[this.position];
    this.position += 1;
    return token;
  }

  private peekIs(...expected: string[]): boolean {
    return expected.includes(this.tokens[this.position]);
  }

  private startsOperand(): boolean {
    const next = this.tokens[this.position];
    return next !== undefined && (/^[\da-z]/.test(next) || next === '(');
  }

  private expect(token: string): void {
    if (this.tokens[this.position] !== token) {
      throw this.invalid();
    }
    this.position += 1;
  }

  private invalid(): ActionConfigError {
    return new ActionConfigError(INVALID_MESSAGE);
  }
}
