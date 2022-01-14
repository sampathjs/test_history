import { InputHTMLAttributes } from 'react';
import { FieldValues, UseFormRegister } from 'react-hook-form';

import { OrderSide } from 'types';

import * as Styles from './Toggle.styles';

export interface ToggleItemProps extends InputHTMLAttributes<HTMLInputElement> {
  value: string;
  side?: OrderSide;
  name?: string;
  register?: UseFormRegister<FieldValues>;
}

export const ToggleItem = ({
  children,
  name,
  register,
  side,
  value,
  ...inputProps
}: React.PropsWithChildren<ToggleItemProps>) => (
  <Styles.Label role="radio" side={side}>
    <input
      type="radio"
      value={value}
      {...(name && register ? { ...register(name) } : {})}
      {...inputProps}
    />
    <Styles.ItemText>{children || value}</Styles.ItemText>
  </Styles.Label>
);
