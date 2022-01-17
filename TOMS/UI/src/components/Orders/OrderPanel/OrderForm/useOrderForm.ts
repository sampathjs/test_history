import { useForm, UseFormProps } from 'react-hook-form';

import { RawFormInputs } from './types';

export const useOrderForm = (props: UseFormProps<RawFormInputs> = {}) => {
  return useForm<RawFormInputs>({
    mode: 'onChange',
    reValidateMode: 'onBlur',
    ...props,
  });
};
