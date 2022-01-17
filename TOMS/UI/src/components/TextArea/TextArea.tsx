import { ChangeEventHandler, forwardRef } from 'react';

import { NO_VALUE } from 'constants/format';

import * as Styles from './TextArea.styles';

export type TextAreaProps = {
  name?: string;
  disabled?: boolean;
  value?: string;
  placeholder?: string;
  onChange?: ChangeEventHandler<HTMLTextAreaElement>;
  className?: string;
};

export const TextArea = forwardRef<HTMLTextAreaElement, TextAreaProps>(
  (
    // eslint-disable-next-line react/prop-types
    { disabled, name, onChange, placeholder = NO_VALUE, value, className },
    ref
  ) => (
    <Styles.Input
      ref={ref}
      name={name}
      disabled={disabled}
      value={value}
      placeholder={placeholder}
      onChange={onChange}
      className={className}
    />
  )
);

TextArea.displayName = 'TextArea';
