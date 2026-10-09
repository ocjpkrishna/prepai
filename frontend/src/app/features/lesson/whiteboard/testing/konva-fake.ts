/**
 * A stand-in for the Konva classes the whiteboard uses. jsdom has no 2D canvas, so the specs
 * replace the module with this: it records what was added and performs no drawing.
 */
export class FakeNode {
  children: FakeNode[] = [];
  attrs: Record<string, unknown>;

  constructor(attrs: Record<string, unknown> = {}) {
    this.attrs = { ...attrs };
  }

  add(child: FakeNode): this {
    this.children.push(child);
    return this;
  }

  destroyChildren(): this {
    this.children = [];
    return this;
  }

  destroy(): void {
    this.children = [];
  }

  batchDraw(): void {
    // Nothing to paint in tests.
  }

  moveToTop(): this {
    return this;
  }

  opacity(value: number): this {
    this.attrs['opacity'] = value;
    return this;
  }

  visible(value: boolean): this {
    this.attrs['visible'] = value;
    return this;
  }

  position(value: unknown): this {
    this.attrs['position'] = value;
    return this;
  }

  fill(value: string): this {
    this.attrs['fill'] = value;
    return this;
  }

  width(value: number): this {
    this.attrs['width'] = value;
    return this;
  }

  height(value: number): this {
    this.attrs['height'] = value;
    return this;
  }

  scale(value: unknown): this {
    this.attrs['scale'] = value;
    return this;
  }
}

export class Stage extends FakeNode {}
export class Layer extends FakeNode {}
export class Group extends FakeNode {}
export class Circle extends FakeNode {}
export class Line extends FakeNode {}
export class Arrow extends FakeNode {}
export class Text extends FakeNode {}
export class Rect extends FakeNode {}

/** Konva's real default export is an object of these classes; konva-shapes.ts reads it. */
export default { Stage, Layer, Group, Circle, Line, Arrow, Text, Rect };
