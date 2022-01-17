import get from 'lodash/get';
import { DefaultTheme, ThemeProps } from 'styled-components/macro';

import { Paths, PathValue } from 'types/util';

import {
  ThemeColors,
  ThemeGradients,
  ThemeShadows,
  ThemeTypography,
} from './types';

export const getColor =
  <Path extends Paths<ThemeColors>>(path: Path) =>
  (props: ThemeProps<DefaultTheme>): PathValue<Path, ThemeColors> => {
    return get(props.theme, `colors.${path}`);
  };

export const getFont =
  <Path extends Paths<ThemeTypography>>(path: Path) =>
  (props: ThemeProps<DefaultTheme>): PathValue<Path, ThemeTypography> => {
    return get(props.theme, `typography.${path}`);
  };

export const getGradient =
  <Path extends Paths<ThemeGradients>>(path: Path) =>
  (props: ThemeProps<DefaultTheme>): PathValue<Path, ThemeGradients> => {
    return get(props.theme, `gradients.${path}`);
  };

export const getShadow =
  <Path extends Paths<ThemeShadows>>(path: Path) =>
  (props: ThemeProps<DefaultTheme>): PathValue<Path, ThemeShadows> => {
    return get(props.theme, `shadows.${path}`);
  };
