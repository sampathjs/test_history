import { MouseEvent } from 'react';

import { Filter, FilterOptionType } from 'types';

import {
  ALL_KEY,
  ALL_VALUE,
  areAllSelected,
  formatOptionValues,
  getNewFilterValue,
  isOptionSelected,
} from '../helpers';
import { Lozenge } from '../Lozenge';
import { LozengeDate } from '../LozengeDate';
import * as Styles from './LozengeFilter.styles';

export const LozengeFilter = ({
  allOptionLabel,
  isExclusive,
  name,
  onChange,
  options,
  shouldEnforceValue,
  value,
}: Filter) => {
  const areAllOptionsSelected = areAllSelected(options, value);

  const handleSelect = (dates: string[]) => {
    onChange(
      name,
      getNewFilterValue(
        dates.join(','),
        options,
        value,
        isExclusive,
        shouldEnforceValue
      )
    );
  };

  const handleClick = (event: MouseEvent<HTMLButtonElement>) =>
    onChange(
      name,
      getNewFilterValue(
        event.currentTarget?.value,
        options,
        value,
        isExclusive,
        shouldEnforceValue
      )
    );

  return (
    <Styles.List>
      {allOptionLabel && (
        <li key={ALL_KEY}>
          <Lozenge
            isActive={areAllOptionsSelected}
            value={formatOptionValues(ALL_VALUE)}
            onClick={handleClick}
          >
            {allOptionLabel}
          </Lozenge>
        </li>
      )}

      {options.map((option) => {
        return (
          <li key={`${option.name}-${formatOptionValues(option.value)}`}>
            {option?.type === FilterOptionType.DATE_PICKER_RANGE ? (
              <LozengeDate value={value} onSelect={handleSelect}>
                {option.name}
              </LozengeDate>
            ) : (
              <Lozenge
                isActive={isOptionSelected(
                  option,
                  value,
                  areAllOptionsSelected,
                  allOptionLabel
                )}
                value={formatOptionValues(option.value)}
                onClick={handleClick}
              >
                {option.name}
              </Lozenge>
            )}
          </li>
        );
      })}
    </Styles.List>
  );
};
