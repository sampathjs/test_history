import { ChangeEvent, useState } from 'react';

import { SelectAll } from 'components/SelectAll';
import { MAX_UNEXPANDED_FILTER_LIST_OPTIONS } from 'constants/filters';
import { Filter } from 'types';

import {
  ALL_KEY,
  ALL_VALUE,
  areAllSelected,
  formatOptionValues,
  getNewFilterValue,
} from '../helpers';
import * as Styles from './ListFilter.styles';
import { ListFilterGroups } from './ListFilterGroups';
import { ListFilterOptions } from './ListFilterOptions';

export const ListFilter = ({
  allOptionLabel,
  isCollapsable,
  isExclusive,
  name,
  onChange,
  optionGroups,
  options,
  shouldEnforceValue,
  value,
}: Filter) => {
  const [isExpanded, setIsExpanded] = useState(false);
  const areAllOptionsSelected = areAllSelected(options, value);

  const handleChange = (event: ChangeEvent<HTMLInputElement>) =>
    onChange(
      name,
      getNewFilterValue(
        event.target?.value,
        options,
        value,
        isExclusive,
        shouldEnforceValue
      )
    );

  const showAllOptions = Boolean(isExpanded || isCollapsable);

  return (
    <>
      <Styles.List>
        {allOptionLabel && (
          <SelectAll
            label={allOptionLabel}
            name={ALL_KEY}
            value={formatOptionValues(ALL_VALUE)}
            checked={areAllOptionsSelected}
            onChange={handleChange}
          />
        )}
        {optionGroups ? (
          <ListFilterGroups
            showAllOptions={showAllOptions}
            options={options}
            optionGroups={optionGroups}
            handleChange={handleChange}
            areAllOptionsSelected={areAllOptionsSelected}
            allOptionLabel={allOptionLabel}
            value={value}
          />
        ) : (
          <ListFilterOptions
            showAllOptions={showAllOptions}
            options={options}
            handleChange={handleChange}
            areAllOptionsSelected={areAllOptionsSelected}
            allOptionLabel={allOptionLabel}
            value={value}
          />
        )}
      </Styles.List>
      {!isCollapsable &&
        options.length > MAX_UNEXPANDED_FILTER_LIST_OPTIONS && (
          <Styles.ShowMore onClick={() => setIsExpanded(!isExpanded)}>
            {isExpanded ? 'Show Less' : 'Show More'}
          </Styles.ShowMore>
        )}
    </>
  );
};
