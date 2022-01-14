import { ChangeEventHandler, ReactNode } from 'react';

import { FilterOption, FilterValue } from 'types';

import { formatOptionValues, isOptionSelected } from '../../helpers';
import * as Styles from './ListFilterOption.styles';

export type ListFilterOptionProps = {
  option: FilterOption;
  value: FilterValue;
  areAllOptionsSelected: boolean;
  handleChange: ChangeEventHandler<HTMLInputElement>;
  allOptionLabel?: string;
  children?: ReactNode | string;
};

export const ListFilterOption = ({
  allOptionLabel,
  areAllOptionsSelected,
  children,
  handleChange,
  option,
  value,
}: ListFilterOptionProps) => {
  const isActive = isOptionSelected(
    option,
    value,
    areAllOptionsSelected,
    allOptionLabel
  );

  return (
    <li>
      <Styles.Label
        $isActive={isActive}
        $areAllSelected={areAllOptionsSelected}
      >
        <Styles.Input
          name={option.name}
          type="checkbox"
          checked={isActive}
          value={formatOptionValues(option.value)}
          onChange={handleChange}
        />
        {children || <span>{option.name}</span>}
      </Styles.Label>
    </li>
  );
};
