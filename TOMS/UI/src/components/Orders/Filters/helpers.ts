import concat from 'lodash/concat';
import difference from 'lodash/difference';
import flatMap from 'lodash/flatMap';
import flattenDeep from 'lodash/flattenDeep';
import intersection from 'lodash/intersection';
import isEmpty from 'lodash/isEmpty';
import isEqual from 'lodash/isEqual';
import isNil from 'lodash/isNil';
import isString from 'lodash/isString';
import without from 'lodash/without';

import {
  isBothOrderType,
  isLimitOrderType,
  isMatureOrdersOn,
  isRefOrderType,
} from 'contexts/Filters/rules';
import {
  Filter,
  FilterIds,
  FilterOption,
  FilterOptionValue,
  FilterQueryPair,
  FilterValue,
  FilterValues,
  OrderStatusIds,
} from 'types';

export const ALL_KEY = 'all';
export const ALL_VALUE = -1;

export const areAllSelected = (options: FilterOption[], value: FilterValue) =>
  difference(
    options.flatMap((option) => option.value),
    flatMap(value)
  ).length === 0;

export const getNewFilterValue = (
  eventValue: string,
  options: FilterOption[],
  value: FilterValue,
  isExclusive?: boolean,
  shouldEnforceValue?: boolean
) => {
  const update = parseOptionValues(eventValue);

  // Reset state if no value
  if (isNil(update)) {
    return [];
  }

  const areAllOptionsSelected = areAllSelected(options, value);

  let newValue;

  // If ALL option clicked
  if (update === ALL_VALUE) {
    return areAllOptionsSelected && !shouldEnforceValue
      ? []
      : options.map((option) => option.value);
  }

  // Exclusive-selection
  if (isExclusive) {
    if (areAllOptionsSelected || !value.includes(update)) {
      newValue = [update];
      return newValue;
    }

    return shouldEnforceValue ? value : [];
  }

  // Multi-selection
  if (areAllOptionsSelected) {
    newValue = [update];
  } else {
    if (Array.isArray(update)) {
      // Check if update already exists in value, and remove
      newValue = value.reduce<FilterValue>(
        (acc, optionValue) =>
          Array.isArray(optionValue) && isEqual(optionValue, update)
            ? acc
            : [...acc, optionValue],
        []
      );

      // No change? Then add new value to state
      if (isEqual(newValue, value)) {
        newValue = concat(value, [update]);
      }
    } else {
      newValue = value.includes(update)
        ? without(value, update)
        : concat(value, update);
    }
  }

  return newValue;
};

export const isOptionSelected = (
  option: FilterOption,
  value: FilterValue,
  areAllSelected: boolean,
  allOptionLabel?: string
) => {
  const isSelected = Array.isArray(option.value)
    ? intersection(flattenDeep(value), option.value).length > 0
    : value.includes(option.value);

  const areAllOptionsSelectedOptionAvailable = Boolean(allOptionLabel);

  if (!isSelected) {
    return false;
  }

  if (!areAllOptionsSelectedOptionAvailable) {
    return true;
  }

  if (!areAllSelected) {
    return true;
  }

  return false;
};

const mapMatureOrderFilterToStatusIds = (state: FilterValues) =>
  isMatureOrdersOn(state)
    ? isBothOrderType(state)
      ? [OrderStatusIds.LIMIT_MATURED, OrderStatusIds.REFERENCE_MATURED]
      : isLimitOrderType(state)
      ? [OrderStatusIds.LIMIT_MATURED]
      : isRefOrderType(state)
      ? [OrderStatusIds.REFERENCE_MATURED]
      : undefined
    : undefined;

export const mapAllSelectedToEmptyArray = (
  filters: Filter[],
  state: FilterValues
) => {
  const filterQueryPairs = Object.entries(state).reduce<FilterQueryPair[]>(
    (acc, [name, value]) => {
      const filter = filters.find((filter) => filter.name === name);

      if (!filter || isEmpty(value)) {
        return acc;
      }

      const values = splitCommaSeparatedValues(value);
      const hasMultipleQueryIds = filter.queryIds.length > 1;

      const queryValuePairs = filter.queryIds.reduce<FilterQueryPair[]>(
        (entries, queryId, index) => {
          // If we have multiple query ids, split values accordingly
          if (hasMultipleQueryIds) {
            return !values?.[index] || areAllSelected(filter.options, values)
              ? entries
              : [...entries, [queryId, [values?.[index]]]];
          }

          // NOTE: Mature orders are sent by default to the server so we must
          // send all other status ids when all options are selected and not
          // leave it blank which is default behaviour (null = All)
          if (
            name === FilterIds.OPEN_ORDER_STATUS ||
            name === FilterIds.CLOSED_ORDER_STATUS
          ) {
            return [...entries, [queryId, values]];
          }

          // NOTE: Map Mature Order filter's On/Off values to correct Mature
          // status ids and inject into queryId for order statuses
          if (name === FilterIds.MATURE_ORDERS) {
            const matureOrderStatusIds = mapMatureOrderFilterToStatusIds(state);

            return matureOrderStatusIds
              ? [...entries, [queryId, matureOrderStatusIds]]
              : entries;
          }

          if (!values || areAllSelected(filter.options, values)) {
            return entries;
          }

          return [...entries, [queryId, values]];
        },
        []
      );

      if (!queryValuePairs.length) {
        return acc;
      }

      return [...acc, ...queryValuePairs];
    },
    []
  );

  return mergeDuplicateQueryIdPairs(filterQueryPairs);
};

const mergeDuplicateQueryIdPairs = (filterQueryPairs: FilterQueryPair[]) =>
  filterQueryPairs.reduce<FilterQueryPair[]>((acc, [queryId, values]) => {
    const entryIndex = acc.findIndex(
      ([existingQueryId]) => existingQueryId === queryId
    );

    if (entryIndex !== -1) {
      const [existingQueryId, existingValues] = acc[entryIndex];
      acc[entryIndex] = [existingQueryId, [...existingValues, ...values]];
      return acc;
    }

    return [...acc, [queryId, values]];
  }, []);

export const splitCommaSeparatedValues = (filterValue: FilterValue) =>
  filterValue.flatMap((value) =>
    isString(value) ? (value.split(',') as FilterValue) : value
  );

export const formatOptionValues = (value: FilterOptionValue) => {
  return Array.isArray(value) ? value.join(',') : `${value}`;
};

export const parseOptionValues = (eventValue: string) => {
  // Check if value contains multiple ids
  const update = eventValue.includes(',') ? eventValue.split(',') : eventValue;

  let newValue: FilterOptionValue | undefined;

  if (Array.isArray(update)) {
    newValue = update.map(convertToNumber);
  } else {
    newValue = update ? convertToNumber(update) : undefined;
  }

  return newValue;
};

export const convertToNumber = (value: string | number) =>
  isNaN(value as unknown as number) ? value : Number(value);
