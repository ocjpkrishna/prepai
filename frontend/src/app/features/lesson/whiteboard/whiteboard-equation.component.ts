import { Component, ElementRef, effect, inject, input } from '@angular/core';
import { EquationRendererService } from './equation-renderer.service';

/** One KaTeX equation in the equation panel. It is fitted to the panel width. */
@Component({
  selector: 'app-whiteboard-equation',
  template: '',
  styleUrl: './whiteboard-equation.component.scss',
  host: {
    class: 'wb-equation',
    '[class.highlight]': 'highlight()',
  },
})
export class WhiteboardEquationComponent {
  readonly latex = input.required<string>();
  readonly highlight = input(false);
  readonly maxWidth = input.required<number>();

  constructor() {
    const host = inject<ElementRef<HTMLElement>>(ElementRef).nativeElement;
    const equations = inject(EquationRendererService);
    effect(() => equations.renderInto(host, this.latex(), this.maxWidth()));
  }
}
