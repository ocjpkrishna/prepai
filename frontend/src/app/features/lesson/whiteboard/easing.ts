/** Maps linear progress (0 to 1) to eased progress (0 to 1). */
export type Easing = (progress: number) => number;

export const linear: Easing = (progress) => progress;

export const easeOutCubic: Easing = (progress) => 1 - (1 - progress) ** 3;

export const easeInOut: Easing = (progress) =>
  progress < 0.5 ? 4 * progress ** 3 : 1 - (-2 * progress + 2) ** 3 / 2;
