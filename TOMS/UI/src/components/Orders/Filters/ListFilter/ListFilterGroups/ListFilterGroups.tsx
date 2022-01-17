import sortBy from 'lodash/sortBy';

import { FilterOption, FilterOptionGroup } from 'types';

import { ListFilterGroup } from '../ListFilterGroup';
import { ListFilterOptionProps } from '../ListFilterOption';

type ListFilterGroupsProps = {
  showAllOptions: boolean;
  options: FilterOption[];
  optionGroups: FilterOptionGroup[];
} & Omit<ListFilterOptionProps, 'option'>;

export const ListFilterGroups = ({
  optionGroups,
  options,
  showAllOptions,
  ...props
}: ListFilterGroupsProps) => {
  let unGroupedOptions: FilterOption[] = options.filter((option) =>
    optionGroups.find((group) => !group.options.includes(option.name))
  );

  // Sort options for nuanced effect
  unGroupedOptions = sortBy(unGroupedOptions, 'name');

  const otherGroup: FilterOptionGroup = {
    subheading: 'Other',
    options: unGroupedOptions.map((option) => option.name),
  };

  return (
    <>
      {optionGroups?.map((optionGroup) => (
        <ListFilterGroup
          key={optionGroup.subheading}
          group={optionGroup}
          options={options.filter((option) =>
            optionGroup.options.includes(option.name)
          )}
          showAllOptions={showAllOptions}
          {...props}
        />
      ))}
      {showAllOptions && unGroupedOptions.length > 1 && (
        <ListFilterGroup
          key="other"
          group={otherGroup}
          options={unGroupedOptions}
          showAllOptions={showAllOptions}
          {...props}
        />
      )}
    </>
  );
};
