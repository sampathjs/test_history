import { MAX_UNEXPANDED_FILTER_LIST_OPTIONS } from 'constants/filters';
import { FilterOption, FilterOptionGroup } from 'types';

import { formatOptionValues } from '../../helpers';
import { TickerItem } from '../../TickerItem';
import { ListFilterOption, ListFilterOptionProps } from '../ListFilterOption';
import * as Styles from './ListFilterGroup.styles';

type ListFilterGroupProps = {
  showAllOptions: boolean;
  group: FilterOptionGroup;
  options: FilterOption[];
} & Omit<ListFilterOptionProps, 'option'>;

export const ListFilterGroup = ({
  group,
  options,
  showAllOptions,
  ...props
}: ListFilterGroupProps) => {
  const visibleOptions = showAllOptions
    ? options
    : options.slice(0, MAX_UNEXPANDED_FILTER_LIST_OPTIONS);

  return (
    <>
      <Styles.GroupSubheading>{group.subheading}</Styles.GroupSubheading>
      {visibleOptions.map(
        (option, index) =>
          visibleOptions && (
            <ListFilterOption
              key={`${option.name}-${formatOptionValues(option.value)}`}
              option={option}
              {...props}
            >
              <TickerItem
                option={option}
                previousOption={options?.[index - 1]}
              />
            </ListFilterOption>
          )
      )}
    </>
  );
};
