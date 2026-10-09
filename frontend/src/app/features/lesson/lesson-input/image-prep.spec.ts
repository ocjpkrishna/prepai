import { fitWithin, isSupportedImage, MAX_IMAGE_EDGE_PX } from './image-prep';

describe('image preparation', () => {
  it('scales the longest edge down to the limit and keeps the ratio', () => {
    expect(fitWithin({ width: 3200, height: 1600 }, MAX_IMAGE_EDGE_PX)).toEqual({ width: 1600, height: 800 });
  });

  it('never enlarges a small photo', () => {
    expect(fitWithin({ width: 800, height: 600 }, MAX_IMAGE_EDGE_PX)).toEqual({ width: 800, height: 600 });
  });

  it('accepts only JPEG, PNG and WebP', () => {
    expect(isSupportedImage('image/jpeg')).toBe(true);
    expect(isSupportedImage('image/png')).toBe(true);
    expect(isSupportedImage('image/webp')).toBe(true);
    expect(isSupportedImage('image/gif')).toBe(false);
  });
});
