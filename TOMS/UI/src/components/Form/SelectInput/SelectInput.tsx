import {
  FieldValues,
  useController,
  UseControllerProps,
} from 'react-hook-form';

import { Select, SelectProps } from 'components/Select';

type SelectInputProps<T> = Pick<SelectProps, 'options' | 'placeholder'> &
  UseControllerProps<T>;

export const SelectInput = <T extends FieldValues>({
  options,
  placeholder,
  ...props
}: SelectInputProps<T>) => {
  const { field } = useController(props);
  return <Select {...field} options={options} placeholder={placeholder} />;
};
