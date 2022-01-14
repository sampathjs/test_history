import { ReactNode } from 'react';

import { CheckboxCheckedIcon, CheckboxIcon } from 'components/Icon';

import * as Styles from './Checkbox.styles';

type CheckboxProps = {
  checked: boolean;
  name?: string;
  value?: string | number;
  onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void;
  defaultChecked?: boolean;
  disabled?: boolean;
  checkedIcon?: ReactNode;
  icon?: ReactNode;
  className?: string;
};

export const Checkbox = ({
  checked,
  checkedIcon = <CheckboxCheckedIcon />,
  className,
  defaultChecked,
  disabled,
  icon = <CheckboxIcon />,
  name,
  onChange,
  value,
}: CheckboxProps) => {
  const isChecked = checked ?? defaultChecked;

  return (
    <Styles.Wrapper $isChecked={isChecked} className={className}>
      {isChecked && checkedIcon}
      {!isChecked && icon}
      <Styles.Input
        type="checkbox"
        name={name}
        value={value}
        checked={isChecked}
        onChange={onChange}
        disabled={disabled}
      />
    </Styles.Wrapper>
  );
};
