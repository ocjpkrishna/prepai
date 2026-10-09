import { Circle, Group, Layer } from './konva-shapes';
import { Point } from '../../../core/models/canvas-action.model';
import { EquationRendererService } from './equation-renderer.service';
import { CANVAS_WIDTH, DEFAULT_FONT_SIZE, PEN_RADIUS } from './whiteboard.constants';

export interface LatexStyle {
  fontSize: number;
  opacity: number;
}

interface LatexItem {
  element: HTMLElement;
  latex: string;
  at: Point;
  fontSize: number;
  opacity: number;
  valid: boolean;
}

/**
 * What is on the board: one Konva group per action (for FADE_OUT and CLEAR_CANVAS by id),
 * the KaTeX overlay for WRITE_LATEX, and the pen tip. Canvas units are scaled here.
 */
export class WhiteboardScene {
  private readonly groups = new Map<string, Group>();
  private readonly latexItems = new Map<string, LatexItem>();
  private readonly pen = new Circle({ radius: PEN_RADIUS, visible: false });
  private scale = 1;

  constructor(
    private readonly layer: Layer,
    private readonly latexLayer: HTMLElement,
    private readonly equations: EquationRendererService,
  ) {
    this.layer.add(this.pen);
  }

  setScale(scale: number): void {
    this.scale = scale;
    this.latexItems.forEach((item) => this.renderLatex(item));
    this.layer.batchDraw();
  }

  groupFor(id: string): Group {
    const existing = this.groups.get(id);
    if (existing) {
      return existing;
    }
    const group = new Group();
    this.layer.add(group);
    this.groups.set(id, group);
    return group;
  }

  resetGroup(id: string): void {
    this.groups.get(id)?.destroyChildren();
  }

  /** Returns false when the LaTeX did not parse and the raw source is shown instead. */
  placeLatex(id: string, latex: string, at: Point, style: LatexStyle): boolean {
    const item = this.latexItems.get(id) ?? this.addLatexItem(id, latex, at);
    item.at = at;
    item.fontSize = style.fontSize;
    item.opacity = style.opacity;
    this.layoutLatex(item);
    return item.valid;
  }

  fade(ids: string[], opacity: number): void {
    ids.forEach((id) => {
      this.groups.get(id)?.opacity(opacity);
      const item = this.latexItems.get(id);
      if (item) {
        item.opacity = opacity;
        this.layoutLatex(item);
      }
    });
  }

  remove(ids: string[]): void {
    ids.forEach((id) => this.removeElement(id));
  }

  clearExcept(keep: string[]): void {
    const ids = [...this.groups.keys(), ...this.latexItems.keys()];
    ids.filter((id) => !keep.includes(id)).forEach((id) => this.removeElement(id));
  }

  clearAll(): void {
    this.clearExcept([]);
  }

  hasContent(): boolean {
    return this.groups.size > 0 || this.latexItems.size > 0;
  }

  setLayerOpacity(opacity: number): void {
    this.layer.opacity(opacity);
  }

  penTo(point: Point | null, color: string): void {
    this.pen.visible(point !== null);
    if (point !== null) {
      this.pen.position(point);
      this.pen.fill(color);
      this.pen.moveToTop();
    }
  }

  draw(): void {
    this.layer.batchDraw();
  }

  private addLatexItem(id: string, latex: string, at: Point): LatexItem {
    const element = document.createElement('div');
    element.style.position = 'absolute';
    this.latexLayer.appendChild(element);
    const item: LatexItem = { element, latex, at, fontSize: DEFAULT_FONT_SIZE, opacity: 1, valid: true };
    this.latexItems.set(id, item);
    this.renderLatex(item);
    return item;
  }

  private renderLatex(item: LatexItem): void {
    item.valid = this.equations.renderInto(item.element, item.latex, (CANVAS_WIDTH - item.at.x) * this.scale);
    this.layoutLatex(item);
  }

  private layoutLatex(item: LatexItem): void {
    const { element, at } = item;
    element.style.left = `${at.x * this.scale}px`;
    element.style.top = `${at.y * this.scale}px`;
    element.style.fontSize = `${item.fontSize * this.scale}px`;
    element.style.opacity = String(item.opacity);
  }

  private removeElement(id: string): void {
    this.groups.get(id)?.destroy();
    this.groups.delete(id);
    this.latexItems.get(id)?.element.remove();
    this.latexItems.delete(id);
  }
}
