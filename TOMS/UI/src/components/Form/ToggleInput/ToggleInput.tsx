import {
  FieldValues,
  useController,
  UseControllerProps,
} from 'react-hook-form';

import { Toggle, ToggleProps } from 'components/Toggle';

type ToggleInputProps<T> = Pick<ToggleProps, 'children'> &
  UseControllerProps<T>;

export const ToggleInput = <T extends FieldValues>({
  children,
  ...props
}: ToggleInputProps<T>) => {
  const { field } = useController(props);
  return <Toggle {...field}>{children}</Toggle>;
};
