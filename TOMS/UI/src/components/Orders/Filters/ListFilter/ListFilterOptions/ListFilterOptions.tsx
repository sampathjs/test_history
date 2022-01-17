import { MAX_UNEXPANDED_FILTER_LIST_OPTIONS } from 'constants/filters';
import { FilterOption } from 'types';

import { formatOptionValues } from '../../helpers';
import { ListFilterOption, ListFilterOptionProps } from '../ListFilterOption';

type ListFilterOptionsProps = {
  showAllOptions: boolean;
  options: FilterOption[];
} & Omit<ListFilterOptionProps, 'option'>;

export const ListFilterOptions = ({
  options,
  showAllOptions,
  ...props
}: ListFilterOptionsProps) => {
  const visibleOptions = showAllOptions
    ? options
    : options.slice(0, MAX_UNEXPANDED_FILTER_LIST_OPTIONS);

  return (
    <>
      {visibleOptions.map((option) => (
        <ListFilterOption
          key={`${option.name}-${formatOptionValues(option.value)}`}
          option={option}
          {...props}
        />
      ))}
    </>
  );
};
