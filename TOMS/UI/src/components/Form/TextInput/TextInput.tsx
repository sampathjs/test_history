import {
  FieldValues,
  useController,
  UseControllerProps,
} from 'react-hook-form';

import { Input, InputProps } from 'components/Input';

type TextInputProps<T> = UseControllerProps<T> &
  Pick<InputProps, 'type' | 'disabled' | 'placeholder' | 'maxLength'>;

export const TextInput = <T extends FieldValues>({
  disabled,
  maxLength,
  placeholder,
  type,
  ...props
}: TextInputProps<T>) => {
  const { field } = useController(props);
  return (
    <Input
      {...field}
      {...props}
      disabled={disabled}
      maxLength={maxLength}
      placeholder={placeholder}
      type={type}
    />
  );
};
