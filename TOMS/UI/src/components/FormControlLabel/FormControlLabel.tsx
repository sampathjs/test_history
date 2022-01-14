import { ReactNode } from 'react';

import * as Styles from './FormControlLabel.styles';

export type LabelPlacement = 'start' | 'end' | 'top' | 'bottom';

type FormControlLabelProps = {
  control: ReactNode;
  label: string;
  labelPlacement?: LabelPlacement;
  className?: string;
  isOptional?: boolean;
};

export const FormControlLabel = ({
  className,
  control,
  isOptional,
  label,
  labelPlacement = 'end',
}: FormControlLabelProps) => (
  <Styles.Label
    className={className}
    placement={labelPlacement}
    isOptional={isOptional}
  >
    {control} <span>{label}</span>
  </Styles.Label>
);
