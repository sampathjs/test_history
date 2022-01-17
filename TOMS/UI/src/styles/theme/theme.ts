import * as colors from './colors';
import * as gradients from './gradients';
import * as shadows from './shadows';
import * as typography from './typography';

export const theme = {
  ...colors,
  ...gradients,
  ...shadows,
  ...typography,
} as const;
