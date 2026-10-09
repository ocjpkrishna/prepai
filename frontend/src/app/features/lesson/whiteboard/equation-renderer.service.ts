import { Injectable } from '@angular/core';
import katex from 'katex';

/** Spec 3.3 and agent 5 task 10: no trusted HTML from the server, and parse errors show the raw source. */
const KATEX_OPTIONS = { displayMode: true, throwOnError: false, trust: false };
const RAW_LATEX_STYLE = 'margin: 0; font-family: monospace; white-space: pre-wrap; overflow-wrap: anywhere; font-size: 0.9em;';

/** Scale that makes content of `contentWidth` fit `maxWidth`. Never enlarges. */
export function fitScale(contentWidth: number, maxWidth: number): number {
  if (maxWidth <= 0 || !(contentWidth > maxWidth)) {
    return 1;
  }
  return maxWidth / contentWidth;
}

@Injectable()
export class EquationRendererService {
  /** Renders `latex` into `host`. Returns false when the source did not parse and the raw text is shown. */
  renderInto(host: HTMLElement, latex: unknown, maxWidth?: number): boolean {
    const source = typeof latex === 'string' ? latex : '';
    katex.render(source, host, KATEX_OPTIONS);
    if (host.querySelector('.katex-error') !== null) {
      this.showRawLatex(host, source);
      return false;
    }
    host.style.zoom = maxWidth === undefined ? '' : String(fitScale(host.scrollWidth, maxWidth));
    return true;
  }

  private showRawLatex(host: HTMLElement, source: string): void {
    const box = document.createElement('pre');
    box.className = 'wb-raw-latex';
    box.style.cssText = RAW_LATEX_STYLE;
    box.textContent = source;
    host.replaceChildren(box);
    host.style.zoom = '';
  }
}
