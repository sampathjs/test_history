import { ChangeEvent } from 'react';

import { Checkbox } from 'components/Checkbox';
import { FormControlLabel } from 'components/FormControlLabel';

import * as Styles from './SelectAll.styles';

type Props = {
  label: string;
  name?: string;
  value?: string | number;
  checked: boolean;
  onChange(event: ChangeEvent<HTMLInputElement>): void;
};

export const SelectAll = (props: Props) => {
  const { checked, label, name, onChange, value } = props;
  return (
    <Styles.Wrapper>
      <FormControlLabel
        label={label}
        control={
          <Styles.SelectAllCheckbox>
            <Checkbox
              name={name}
              value={value}
              checked={checked}
              onChange={onChange}
            />
          </Styles.SelectAllCheckbox>
        }
      />
    </Styles.Wrapper>
  );
};
