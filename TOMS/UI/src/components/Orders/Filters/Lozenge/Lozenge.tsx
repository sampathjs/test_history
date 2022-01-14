import React from 'react';

import * as Styles from './Lozenge.styles';

type LozengeProps = {
  children: string;
  isActive?: boolean;
  value?: string;
  onClick: (event: React.MouseEvent<HTMLButtonElement>) => void;
};

export const Lozenge = ({
  children,
  isActive,
  onClick,
  value,
}: LozengeProps) => (
  <Styles.Lozenge $isActive={isActive} value={value} onClick={onClick}>
    <Styles.Label>{children}</Styles.Label>
  </Styles.Lozenge>
);
