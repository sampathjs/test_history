import isEqual from 'lodash/isEqual';
import {
  createContext,
  ReactNode,
  useCallback,
  useContext,
  useEffect,
  useReducer,
  useRef,
  useState,
} from 'react';

import { useOrderStatus } from 'hooks/useOrderStatus';
import { useParties } from 'hooks/useParties';
import { useRefData } from 'hooks/useRefData';
import { useUsers } from 'hooks/useUsers';
import { Filter, FilterConfig, FilterValue, FilterValues } from 'types';
import { Nullable } from 'types/util';

import { config } from './config';
import {
  applyFilterRules,
  getFilterDefaultValue,
  mapFiltersConfigOptions,
  mapFiltersWithCallback,
  mapFiltersWithState,
} from './helpers';
import {
  filterReducer,
  initialReducerState,
  setFilterDefaultsAction,
  updateFilterAction,
} from './reducer';

type ContextProps = {
  filters: Filter[];
  state: FilterValues;
};

const FilterContext = createContext<Nullable<ContextProps>>(null);

export const useFilterContext = () => {
  const value = useContext(FilterContext);

  if (!value) {
    throw new Error('useFilterContext must be used within FilterProvider');
  }

  return value;
};

type Props = {
  children?: ReactNode;
};

export const FilterProvider = ({ children }: Props) => {
  const { data: partyData } = useParties();
  const refData = useRefData();

  const { data: userData } = useUsers();
  const { data: orderStatusData } = useOrderStatus();

  const isDefaultValuesSet = useRef(false);
  const [filterConfig, setFilterConfig] = useState<FilterConfig[] | undefined>(
    undefined
  );
  const [state, dispatch] = useReducer(filterReducer, initialReducerState);
  const [filters, setFilters] = useState<Filter[]>([]);

  const handleChange = useCallback((name: string, value: FilterValue) => {
    dispatch(updateFilterAction([{ name: name, value }]));
  }, []);

  // Initial setup of filters - map options from ref and party data
  useEffect(() => {
    if (!refData || !partyData || !userData || !orderStatusData) {
      return;
    }

    const updatedFilterConfig = mapFiltersConfigOptions(
      state && filterConfig ? applyFilterRules(state, filterConfig) : config,
      refData,
      partyData,
      userData,
      orderStatusData
    );

    if (!isEqual(filterConfig, updatedFilterConfig)) {
      setFilterConfig(updatedFilterConfig);
    }
  }, [
    refData,
    partyData,
    userData,
    orderStatusData,
    filterConfig,
    handleChange,
    state,
  ]);

  // Set default values for filters
  useEffect(() => {
    if (!refData || !partyData || !userData || !filterConfig) {
      return;
    }

    if (isDefaultValuesSet.current === false) {
      dispatch(
        setFilterDefaultsAction(
          filterConfig.map((filter) => ({
            name: filter.name,
            value: getFilterDefaultValue(filter),
          }))
        )
      );

      isDefaultValuesSet.current = true;
    }
  }, [refData, partyData, userData, filterConfig]);

  useEffect(() => {
    if (filterConfig) {
      setFilters(filterConfig as Filter[]);
    }
  }, [filterConfig]);

  const prepareFilters = (filters: Filter[]) => {
    let mappedFilters = mapFiltersWithState(filters, state);
    mappedFilters = mapFiltersWithCallback(mappedFilters, state, handleChange);
    mappedFilters = mappedFilters.filter((filter) => {
      if (filter.isHidden !== undefined) {
        return !filter.isHidden;
      }

      return true;
    });

    return mappedFilters;
  };

  return (
    <FilterContext.Provider
      value={{
        filters: prepareFilters(filters),
        state,
      }}
    >
      {children}
    </FilterContext.Provider>
  );
};
