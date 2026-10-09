import { TestBed } from '@angular/core/testing';
import { EquationRendererService, fitScale } from './equation-renderer.service';

describe('EquationRendererService', () => {
  let renderer: EquationRendererService;
  let host: HTMLDivElement;

  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [EquationRendererService] });
    renderer = TestBed.inject(EquationRendererService);
    host = document.createElement('div');
  });

  it('renders valid LaTeX with KaTeX', () => {
    expect(renderer.renderInto(host, '\\frac{1}{2}')).toBe(true);
    expect(host.querySelector('.katex')).not.toBeNull();
  });

  it('shows the raw source in a monospace box when the LaTeX does not parse', () => {
    expect(renderer.renderInto(host, '\\frac{1}{')).toBe(false);
    expect(host.querySelector('.wb-raw-latex')?.textContent).toBe('\\frac{1}{');
    expect(host.querySelector('.katex-error')).toBeNull();
  });

  it('does not turn \\href into a link when trust is off', () => {
    renderer.renderInto(host, '\\href{javascript:alert(1)}{x}');
    expect(host.querySelector('a')).toBeNull();
  });

  it('treats a non-string as empty LaTeX', () => {
    expect(renderer.renderInto(host, 42)).toBe(true);
  });
});

describe('fitScale', () => {
  it('shrinks content wider than the panel', () => {
    expect(fitScale(600, 300)).toBeCloseTo(0.5);
  });

  it('leaves content that fits alone', () => {
    expect(fitScale(200, 300)).toBe(1);
  });

  it('never enlarges, even when nothing was measured', () => {
    expect(fitScale(0, 300)).toBe(1);
  });
});
