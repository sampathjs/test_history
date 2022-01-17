import { Collapsable } from 'components/Collapsable';
import { ChevronDownIcon, ChevronUpIcon } from 'components/Icon';
import { useFilterContext, useLayoutContext } from 'contexts';
import { Filter, FilterType } from 'types';

import * as Styles from './Filters.styles';
import { ListFilter } from './ListFilter';
import { LozengeFilter } from './LozengeFilter';

export const Filters = () => {
  const { filters } = useFilterContext();
  const { filter } = useLayoutContext();

  return (
    <Styles.Layout
      $isExpanded={filter.isPanelExpanded}
      $isToggleHover={filter.isToggleHover}
    >
      <Styles.Heading>Filter Options</Styles.Heading>
      {filters.map((filter) => (
        <Styles.Filter key={filter.name}>
          {filter.isCollapsable ? (
            <Collapsable
              collapsedByDefault={filter.isCollapsedByDefault}
              label={
                <Styles.CollapsableSubheading>
                  {filter.heading} <ChevronUpIcon />
                </Styles.CollapsableSubheading>
              }
              collapsedLabel={
                <Styles.CollapsableSubheading>
                  {filter.heading} <ChevronDownIcon />
                </Styles.CollapsableSubheading>
              }
              content={<FilterContent filter={filter} />}
            />
          ) : (
            <>
              <Styles.Subheading>{filter.heading}</Styles.Subheading>
              <FilterContent filter={filter} />
            </>
          )}
        </Styles.Filter>
      ))}
    </Styles.Layout>
  );
};

type FilterContentProps = {
  filter: Filter;
};

const FilterContent = ({ filter }: FilterContentProps) => (
  <>
    {filter.type === FilterType.LOZENGE && <LozengeFilter {...filter} />}
    {filter.type === FilterType.LIST && <ListFilter {...filter} />}
  </>
);
