import { ChangeEventHandler, forwardRef } from 'react';

import { NO_VALUE } from 'constants/format';

import * as Styles from './Input.styles';

export type InputProps = {
  name?: string;
  disabled?: boolean;
  value?: string;
  type?: string;
  placeholder?: string;
  defaultValue?: string;
  maxLength?: number;
  onChange?: ChangeEventHandler<HTMLInputElement>;
};

export const Input = forwardRef<HTMLInputElement, InputProps>(
  (
    {
      /* eslint-disable react/prop-types */
      defaultValue,
      disabled,
      maxLength,
      name,
      onChange,
      placeholder = NO_VALUE,
      type = 'text',
      value,
      /* eslint-enable react/prop-types */
    },
    ref
  ) => (
    <Styles.Input
      ref={ref}
      name={name}
      type={type}
      value={value}
      disabled={disabled}
      defaultValue={defaultValue}
      maxLength={maxLength}
      placeholder={placeholder}
      onChange={onChange}
    />
  )
);

Input.displayName = 'Input';
