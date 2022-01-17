import { forwardRef } from 'react';
import ReactSelect from 'react-select';

import { NO_VALUE } from 'constants/format';
import { Nullable } from 'types/util';

import * as Styles from './Select.styles';

export type OptionValue = Nullable<string | number>;

export interface OptionType {
  label: string;
  value: OptionValue;
}

export const isOptionType = (
  option: OptionType | OptionValue
): option is OptionType => {
  return Boolean(option && typeof option === 'object' && 'value' in option);
};

export interface OptionGroup {
  label: string;
  options: OptionType[];
}

export type SelectProps = {
  name?: string;
  options?: OptionGroup[] | OptionType[];
  value?: OptionType | OptionValue;
  defaultValue?: OptionType | OptionValue;
  disabled?: boolean;
  onChange?: (value: OptionType | OptionValue) => void;
  placeholder?: string;
};

export const Select = forwardRef(
  (
    {
      defaultValue,
      disabled,
      name,
      onChange,
      options = [],
      placeholder = NO_VALUE,
      value,
    }: SelectProps,
    ref
  ) => {
    return (
      <Styles.Wrapper>
        <ReactSelect
          name={name}
          classNamePrefix="react-select"
          isDisabled={disabled}
          value={value}
          placeholder={placeholder}
          options={options}
          onChange={onChange}
          defaultValue={defaultValue}
        />
      </Styles.Wrapper>
    );
  }
);

Select.displayName = 'Select';
