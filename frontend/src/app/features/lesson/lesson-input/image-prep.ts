export const MAX_IMAGE_BYTES = 5 * 1024 * 1024;
export const MAX_IMAGE_EDGE_PX = 1600;

const JPEG_QUALITY = 0.85;
const WHITE = '#ffffff';
const SUPPORTED_IMAGE_TYPES: readonly string[] = ['image/jpeg', 'image/png', 'image/webp'];

export interface Size {
  width: number;
  height: number;
}

export function isSupportedImage(mimeType: string): boolean {
  return SUPPORTED_IMAGE_TYPES.includes(mimeType);
}

/** Scales a size down so its longest edge is at most maxEdge. Images are never enlarged. */
export function fitWithin(size: Size, maxEdge: number): Size {
  const longest = Math.max(size.width, size.height);
  if (longest <= maxEdge) {
    return size;
  }
  const scale = maxEdge / longest;
  return { width: Math.round(size.width * scale), height: Math.round(size.height * scale) };
}

/** Re-encodes a photo as a JPEG no wider or taller than maxEdge, so the upload stays small. */
export async function downscaleImage(file: Blob, maxEdge: number = MAX_IMAGE_EDGE_PX): Promise<Blob> {
  const bitmap = await createImageBitmap(file);
  const target = fitWithin({ width: bitmap.width, height: bitmap.height }, maxEdge);
  const canvas = drawOnWhite(bitmap, target);
  bitmap.close();
  return encodeJpeg(canvas);
}

function drawOnWhite(bitmap: ImageBitmap, target: Size): HTMLCanvasElement {
  const canvas = document.createElement('canvas');
  canvas.width = target.width;
  canvas.height = target.height;
  const context = canvas.getContext('2d');
  if (context) {
    context.fillStyle = WHITE;
    context.fillRect(0, 0, target.width, target.height);
    context.drawImage(bitmap, 0, 0, target.width, target.height);
  }
  return canvas;
}

function encodeJpeg(canvas: HTMLCanvasElement): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob(
      blob => (blob ? resolve(blob) : reject(new Error('The photo could not be prepared'))),
      'image/jpeg',
      JPEG_QUALITY,
    );
  });
}
