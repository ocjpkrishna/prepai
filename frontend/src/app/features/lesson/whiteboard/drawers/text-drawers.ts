import { Text } from '../konva-shapes';
import { Drawer } from '../draw-context';
import {
  DEFAULT_FONT_SIZE,
  MAX_FONT_SIZE,
  MIN_FONT_SIZE,
} from '../whiteboard.constants';
import { addShape } from './drawing-helpers';

export const drawText: Drawer = (context, config, progress) => {
  const position = config.point('position');
  const text = config.text('text');
  const fontSize = config.number('fontSize', { fallback: DEFAULT_FONT_SIZE, min: MIN_FONT_SIZE, max: MAX_FONT_SIZE });
  const color = config.color('color', context.palette.ink);
  addShape(context, new Text({ x: position.x, y: position.y, text, fontSize, fill: color, opacity: progress }));
  return position;
};

/** LaTeX is an HTML overlay, not a Konva shape, so it sits above the canvas and stays crisp. */
export const drawLatex: Drawer = (context, config, progress) => {
  const position = config.point('position');
  const latex = config.text('latex');
  const fontSize = config.number('fontSize', { fallback: DEFAULT_FONT_SIZE, min: MIN_FONT_SIZE, max: MAX_FONT_SIZE });
  if (!context.scene.placeLatex(context.id, latex, position, { fontSize, opacity: progress })) {
    config.notes.push('latex does not parse, raw source shown');
  }
  return position;
};
