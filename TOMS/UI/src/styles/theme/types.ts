import { theme } from './theme';

export type Theme = typeof theme;
export type ThemeColors = Theme['colors'];
export type ThemeGradients = Theme['gradients'];
export type ThemeShadows = Theme['shadows'];
export type ThemeTypography = Theme['typography'];
