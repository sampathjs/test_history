import groupBy from 'lodash/groupBy';
import isEqual from 'lodash/isEqual';
import sortBy from 'lodash/sortBy';

import { ALL_VALUE } from 'components/Orders/Filters/helpers';
import {
  Filter,
  FilterConfig,
  FilterIds,
  FilterOption,
  FilterOptionType,
  FilterValue,
  FilterValues,
  OffOnIds,
  OrderStatus,
  OrderStatusCategoryIds,
  OrderStatusIds,
  OrderTypeNameIds,
  Party,
  PartyTypeIds,
  RefDataMap,
  RefIds,
  User,
} from 'types';
import {
  API_DATE_TIME_FORMAT,
  getDateLastMonthStart,
  getDateToday,
} from 'utils/date';
import { getRefDataByTypeId } from 'utils/references';

export const getPartyDataByTypeId = (
  data: Party[] | undefined,
  typeId: PartyTypeIds | undefined
) => (data && typeId ? data.filter((party) => party.typeId === typeId) : []);

export const getOrderStatusesByCategoryId = (
  data: OrderStatus[],
  typeId: OrderStatusCategoryIds | undefined,
  orderTypeNameId: OrderTypeNameIds[] | undefined
) => {
  const list = typeId
    ? data.filter(
        (orderStatus) =>
          orderStatus.idOrderTypeCategory === typeId &&
          ![
            OrderStatusIds.LIMIT_MATURED,
            OrderStatusIds.REFERENCE_MATURED,
          ].includes(Number(orderStatus.id))
      )
    : [];

  const filteredByOrderType = list.filter(({ idOrderTypeName }) =>
    orderTypeNameId?.includes(idOrderTypeName)
  );

  return filteredByOrderType;
};

export const mapFilterOptions = (
  filter: FilterConfig,
  refData: RefDataMap,
  partyData: Party[],
  userData: User[],
  orderStatusData: OrderStatus[]
) => {
  const { ids, name } = filter;

  switch (name) {
    // Custom filters
    case FilterIds.EXECUTED_AT:
      const dateTodayStr = getDateToday().toFormat(API_DATE_TIME_FORMAT);
      const lastMonthStr =
        getDateLastMonthStart().toFormat(API_DATE_TIME_FORMAT);

      return [
        {
          name: 'Today',
          value: dateTodayStr,
        },
        {
          name: 'Last Month',
          value: lastMonthStr,
        },
        {
          name: 'Custom',
          value: [],
          type: FilterOptionType.DATE_PICKER_RANGE,
        },
      ];

    case FilterIds.MATURE_ORDERS:
      return [
        { name: 'Off', value: OffOnIds.OFF },
        { name: 'On', value: OffOnIds.ON },
      ];

    // Party data driven filters
    case FilterIds.INTERNAL_BUSINESS_UNIT:
    case FilterIds.EXTERNAL_BUSINESS_UNIT:
      return getPartyDataByTypeId(partyData, ids?.partyTypeId).map(
        ({ id, name }) => ({
          name,
          value: id,
        })
      );

    // User data driven filters
    case FilterIds.SUBMITTERS:
      return sortBy(
        userData.map(({ firstName, id, lastName }) => ({
          name: `${firstName} ${lastName}`,
          value: id,
        })),
        'name'
      );

    case FilterIds.OPEN_ORDER_STATUS:
    case FilterIds.CLOSED_ORDER_STATUS:
      const groupedByStatus = groupBy(
        getOrderStatusesByCategoryId(
          orderStatusData,
          ids?.orderStatusCategoryId,
          ids?.orderTypeNameId
        ),
        (status) => status.orderStatusName
      );
      return Object.entries(groupedByStatus).map(([status, group]) => ({
        name: `${status}`,
        value:
          group.length > 1 ? group.map((option) => option.id) : group[0].id,
      }));

    // Ref data driven filters
    default:
      return getRefDataByTypeId(refData, ids?.refTypeId).map(
        ({ displayName, id, name }) => ({
          name: displayName ?? name,
          value: id,
        })
      );
  }
};

export const mapFilterOptionLabels = (option: FilterOption) => {
  if (option.value === RefIds.LIMIT_ORDER) {
    return { ...option, name: 'Limit' };
  }
  if (option.value === RefIds.REF_ORDER) {
    return { ...option, name: 'Reference' };
  }
  return option;
};

export const mapFiltersConfigOptions = (
  config: FilterConfig[],
  refData: RefDataMap,
  partyData: Party[],
  userData: User[],
  orderStatusData: OrderStatus[]
) =>
  config.map((filter) => ({
    ...filter,
    options: mapFilterOptions(
      filter,
      refData,
      partyData,
      userData,
      orderStatusData
    )?.map(mapFilterOptionLabels),
  }));

export const mapFiltersWithCallback = (
  config: Filter[],
  state: FilterValues,
  onChange: (name: string, value: FilterValue) => void
) =>
  config.map((filter) => ({
    ...filter,
    onChange: (name: string, value: FilterValue) => {
      onChange(name, value);

      let updatedState: FilterValues = { ...state, [name]: value };

      const sideEffects = filter
        ?.sideEffects?.(updatedState, filter)
        ?.filter(({ condition }) => condition)
        ?.flatMap(({ result }) => result);

      if (sideEffects?.length) {
        Object.entries(groupBy(sideEffects, 'name')).forEach(
          ([name, results]) => {
            let newValue: FilterValue;

            results.forEach(({ callback, value }) => {
              if (value) {
                newValue = value;
              } else if (callback) {
                newValue = callback(updatedState);
              }

              if (isEqual(newValue, [ALL_VALUE])) {
                const affectedFilter = config.find(
                  (filter) => filter.name === name
                );

                const defaultValues = affectedFilter
                  ? getFilterDefaultValue(affectedFilter)
                  : undefined;

                if (defaultValues) {
                  newValue = defaultValues;
                }
              }

              if (newValue) {
                updatedState = { ...updatedState, [name]: newValue };
              }
            });

            if (!isEqual(state[name], updatedState[name])) {
              onChange(name, updatedState[name]);
            }
          }
        );
      }
    },
  }));

export const mapFiltersWithState = (config: Filter[], state: FilterValues) =>
  config.map((filter) => ({
    ...filter,
    value: state[filter.name],
  }));

export const applyFilterRules = (
  state: FilterValues,
  filters: FilterConfig[]
) =>
  filters.map((filter) =>
    filter?.rules
      ? {
          ...filter,
          ...filter
            .rules(state)
            .reduce(
              (acc, { condition, result }) =>
                condition ? { ...acc, ...result } : acc,
              {}
            ),
        }
      : filter
  );

export const getFilterDefaultValue = (filter: FilterConfig) => {
  if (!filter.options) {
    return [];
  }

  if (filter.defaultValue) {
    return filter.defaultValue;
  }

  if (filter.isExclusive && !filter.allOptionLabel) {
    return filter.options.slice(0, 1).map((option) => option.value);
  }

  return filter.options.map((option) => option.value);
};
