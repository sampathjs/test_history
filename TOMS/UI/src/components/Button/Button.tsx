import { ReactNode } from 'react';
import {
  StyledComponentInnerOtherProps,
  StyledComponentPropsWithRef,
} from 'styled-components/macro';

import * as Styles from './Button.styles';

type Props = StyledComponentPropsWithRef<typeof Styles.Button> &
  Partial<StyledComponentInnerOtherProps<typeof Styles.Button>> & {
    children?: ReactNode;
    startIcon?: ReactNode;
    endIcon?: ReactNode;
  };

export const Button = (props: Props) => {
  const {
    children,
    color = 'primary',
    endIcon,
    size = 'medium',
    startIcon,
    variant = 'contained',
    ...buttonProps
  } = props;

  return (
    <Styles.Button {...buttonProps} variant={variant} color={color} size={size}>
      {startIcon && <Styles.StartIcon>{startIcon}</Styles.StartIcon>}
      {children}
      {endIcon && <Styles.EndIcon>{endIcon}</Styles.EndIcon>}
    </Styles.Button>
  );
};
