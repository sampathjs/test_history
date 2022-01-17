import flatMap from 'lodash/flatMap';
import intersection from 'lodash/intersection';
import isEqual from 'lodash/isEqual';

import {
  Filter,
  FilterIds,
  FilterOptionValue,
  FilterValue,
  FilterValues,
  OffOnIds,
  OrderStatusIds,
  RefIds,
} from 'types';

export const isLimitOrderType = (state: FilterValues) =>
  isEqual(state[FilterIds.ORDER_TYPE], [RefIds.LIMIT_ORDER]);

export const isRefOrderType = (state: FilterValues) =>
  isEqual(state[FilterIds.ORDER_TYPE], [RefIds.REF_ORDER]);

export const isBothOrderType = (state: FilterValues) =>
  isEqual(state[FilterIds.ORDER_TYPE], [RefIds.LIMIT_ORDER, RefIds.REF_ORDER]);

export const isPartFillable = (state: FilterValues) =>
  isEqual(state[FilterIds.PART_FILLABLE], [RefIds.YES]);

export const isNotPartFillable = (state: FilterValues) =>
  isEqual(state[FilterIds.PART_FILLABLE], [RefIds.NO]);

export const isAllRefSource = (state: FilterValues, filter: Filter) =>
  isEqual(
    state[FilterIds.REF_SOURCE],
    filter.options.map((option) => option.value)
  );

export const isAllFxIndexRefSource = (state: FilterValues, filter: Filter) =>
  isEqual(
    state[FilterIds.FX_INDEX],
    filter.options.map((option) => option.value)
  );

export const isAllCurrency = (state: FilterValues, filter: Filter) =>
  isEqual(
    state[FilterIds.CURRENCY],
    filter.options.map((option) => option.value)
  );

export const isAllContractTemplate = (state: FilterValues, filter: Filter) =>
  isEqual(
    state[FilterIds.CONTRACT_TEMPLATE],
    filter.options.map((option) => option.value)
  );

export const isContractTemplateLimit = (state: FilterValues) =>
  isEqual(state[FilterIds.CONTRACT_TEMPLATE], [
    RefIds.CONTRACT_TEMPLATE_RELATIVE,
  ]) ||
  isEqual(state[FilterIds.CONTRACT_TEMPLATE], [
    RefIds.CONTRACT_TEMPLATE_FIXED,
  ]) ||
  isEqual(state[FilterIds.CONTRACT_TEMPLATE], [
    RefIds.CONTRACT_TEMPLATE_RELATIVE,
    RefIds.CONTRACT_TEMPLATE_FIXED,
  ]);

export const isContractTemplateReference = (state: FilterValues) =>
  isEqual(state[FilterIds.CONTRACT_TEMPLATE], [
    RefIds.CONTRACT_TEMPLATE_AVERAGE,
  ]) ||
  isEqual(state[FilterIds.CONTRACT_TEMPLATE], [
    RefIds.CONTRACT_TEMPLATE_FIXING,
  ]) ||
  isEqual(state[FilterIds.CONTRACT_TEMPLATE], [
    RefIds.CONTRACT_TEMPLATE_AVERAGE,
    RefIds.CONTRACT_TEMPLATE_FIXING,
  ]);

export const isStatusLimitOnly = (state: FilterValues) =>
  flatMap(state[FilterIds.OPEN_ORDER_STATUS]).some((status) =>
    [
      OrderStatusIds.LIMIT_CONFIRMED,
      OrderStatusIds.LIMIT_PARTIALLY_FILLED,
    ].includes(Number(status))
  );

export const isStatusLimitPending = (state: FilterValues) =>
  flatMap(state[FilterIds.OPEN_ORDER_STATUS]).includes(
    OrderStatusIds.LIMIT_PENDING
  );

export const isStatusReferencePending = (state: FilterValues) =>
  flatMap(state[FilterIds.OPEN_ORDER_STATUS]).includes(
    OrderStatusIds.REFERENCE_PENDING
  );

export const isStatusLimitPulled = (state: FilterValues) =>
  flatMap(state[FilterIds.CLOSED_ORDER_STATUS]).includes(
    OrderStatusIds.LIMIT_PULLED
  );

export const isStatusReferencePulled = (state: FilterValues) =>
  flatMap(state[FilterIds.CLOSED_ORDER_STATUS]).includes(
    OrderStatusIds.REFERENCE_PULLED
  );

export const isStatusLimitRejected = (state: FilterValues) =>
  flatMap(state[FilterIds.CLOSED_ORDER_STATUS]).includes(
    OrderStatusIds.LIMIT_REJECTED
  );

export const isStatusReferenceRejected = (state: FilterValues) =>
  flatMap(state[FilterIds.CLOSED_ORDER_STATUS]).includes(
    OrderStatusIds.REFERENCE_REJECTED
  );

export const isMatureOrdersOn = (state: FilterValues) =>
  isEqual(state[FilterIds.MATURE_ORDERS], [OffOnIds.ON]);

export const isMatureOrdersOff = (state: FilterValues) =>
  isEqual(state[FilterIds.MATURE_ORDERS], [OffOnIds.OFF]);

/**
 * Result actions
 */

const setStatusTo = (
  state: FilterValue,
  targetStatuses: OrderStatusIds[],
  value: FilterOptionValue
) =>
  state?.map((optionValue) => {
    return Array.isArray(optionValue) &&
      intersection(optionValue, targetStatuses).length > 0
      ? value
      : targetStatuses.includes(Number(optionValue))
      ? value
      : optionValue;
  });

const setOpenStatusTo = (
  state: FilterValues,
  targetStatuses: OrderStatusIds[],
  value: FilterOptionValue
) => setStatusTo(state[FilterIds.OPEN_ORDER_STATUS], targetStatuses, value);

export const doPendingToBoth = (state: FilterValues) =>
  setOpenStatusTo(
    state,
    [OrderStatusIds.LIMIT_PENDING, OrderStatusIds.REFERENCE_PENDING],
    [OrderStatusIds.LIMIT_PENDING, OrderStatusIds.REFERENCE_PENDING]
  );

export const doPendingToLimit = (state: FilterValues) =>
  setOpenStatusTo(
    state,
    [OrderStatusIds.LIMIT_PENDING, OrderStatusIds.REFERENCE_PENDING],
    OrderStatusIds.LIMIT_PENDING
  );

export const doPendingToReference = (state: FilterValues) =>
  setOpenStatusTo(
    state,
    [OrderStatusIds.LIMIT_PENDING, OrderStatusIds.REFERENCE_PENDING],
    OrderStatusIds.REFERENCE_PENDING
  );

const setClosedStatusTo = (
  state: FilterValues,
  targetStatuses: OrderStatusIds[],
  value: FilterOptionValue
) => setStatusTo(state[FilterIds.CLOSED_ORDER_STATUS], targetStatuses, value);

export const doPulledToBoth = (state: FilterValues) =>
  setClosedStatusTo(
    state,
    [OrderStatusIds.LIMIT_PULLED, OrderStatusIds.REFERENCE_PULLED],
    [OrderStatusIds.LIMIT_PULLED, OrderStatusIds.REFERENCE_PULLED]
  );

export const doPulledToLimit = (state: FilterValues) =>
  setClosedStatusTo(
    state,
    [OrderStatusIds.LIMIT_PULLED, OrderStatusIds.REFERENCE_PULLED],
    OrderStatusIds.LIMIT_PULLED
  );

export const doPulledToReference = (state: FilterValues) =>
  setClosedStatusTo(
    state,
    [OrderStatusIds.LIMIT_PULLED, OrderStatusIds.REFERENCE_PULLED],
    OrderStatusIds.REFERENCE_PULLED
  );

export const doRejectedToBoth = (state: FilterValues) =>
  setClosedStatusTo(
    state,
    [OrderStatusIds.LIMIT_REJECTED, OrderStatusIds.REFERENCE_REJECTED],
    [OrderStatusIds.LIMIT_REJECTED, OrderStatusIds.REFERENCE_REJECTED]
  );

export const doRejectedToLimit = (state: FilterValues) =>
  setClosedStatusTo(
    state,
    [OrderStatusIds.LIMIT_REJECTED, OrderStatusIds.REFERENCE_REJECTED],
    OrderStatusIds.LIMIT_REJECTED
  );

export const doRejectedToReference = (state: FilterValues) =>
  setClosedStatusTo(
    state,
    [OrderStatusIds.LIMIT_REJECTED, OrderStatusIds.REFERENCE_REJECTED],
    OrderStatusIds.REFERENCE_REJECTED
  );

export const removeLimitOnlyStatuses = (state: FilterValues) =>
  state[FilterIds.OPEN_ORDER_STATUS].reduce<FilterValue>((acc, status) => {
    if (
      [
        OrderStatusIds.LIMIT_CONFIRMED,
        OrderStatusIds.LIMIT_PARTIALLY_FILLED,
      ].includes(Number(status))
    ) {
      return acc;
    }

    return [...acc, status];
  }, []);
