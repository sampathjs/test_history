import {
  FieldValues,
  useController,
  UseControllerProps,
} from 'react-hook-form';

import { TextArea, TextAreaProps } from 'components/TextArea';

type TextAreaInputProps<T> = UseControllerProps<T> &
  Pick<TextAreaProps, 'placeholder'>;

export const TextAreaInput = <T extends FieldValues>({
  placeholder,
  ...props
}: TextAreaInputProps<T>) => {
  const { field } = useController(props);
  return <TextArea {...field} placeholder={placeholder} />;
};
