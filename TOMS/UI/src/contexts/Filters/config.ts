import { ALL_VALUE } from 'components/Orders/Filters/helpers';
import {
  Filter,
  FilterConfig,
  FilterIds,
  FilterType,
  FilterValues,
  OffOnIds,
  OrderQueryIds,
  OrderStatusCategoryIds,
  OrderTypeNameIds,
  PartyTypeIds,
  RefIds,
  RefTypeIds,
} from 'types';

import {
  doPendingToBoth,
  doPendingToLimit,
  doPendingToReference,
  doPulledToBoth,
  doPulledToLimit,
  doPulledToReference,
  doRejectedToBoth,
  doRejectedToLimit,
  doRejectedToReference,
  isAllCurrency,
  isAllFxIndexRefSource,
  isAllRefSource,
  isBothOrderType,
  isContractTemplateLimit,
  isContractTemplateReference,
  isLimitOrderType,
  isNotPartFillable,
  isPartFillable,
  isRefOrderType,
  isStatusLimitPending,
  isStatusLimitPulled,
  isStatusLimitRejected,
  isStatusReferencePending,
  isStatusReferencePulled,
  isStatusReferenceRejected,
} from './rules';

export const TICKER_POPULAR_OPTIONS = [
  'XPT/USD',
  'XPD/USD',
  'XRH/USD',
  'XIR/USD',
  'XPT/EUR',
  'XPD/EUR',
];

export const config: FilterConfig[] = [
  {
    name: FilterIds.EXECUTED_AT,
    queryIds: [
      OrderQueryIds.MIN_CREATED_AT_DATE,
      OrderQueryIds.MAX_CREATED_AT_DATE,
    ],
    heading: 'Executed Date Range',
    type: FilterType.LOZENGE,
    isExclusive: true,
    defaultValue: [],
  },
  {
    name: FilterIds.INTERNAL_BUSINESS_UNIT,
    queryIds: [OrderQueryIds.INTERNAL_BUSINESS_UNIT],
    heading: 'JM PMM Unit',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    ids: {
      partyTypeId: PartyTypeIds.INTERNAL_BUSINESS_UNIT,
    },
  },
  {
    name: FilterIds.EXTERNAL_BUSINESS_UNIT,
    queryIds: [OrderQueryIds.EXTERNAL_BUSINESS_UNIT],
    heading: 'Counterparty',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    ids: {
      partyTypeId: PartyTypeIds.EXTERNAL_BUSINESS_UNIT,
    },
  },
  {
    name: FilterIds.ORDER_TYPE,
    queryIds: [OrderQueryIds.ORDER_TYPE],
    heading: 'Order Type',
    type: FilterType.LOZENGE,
    allOptionLabel: 'Both',
    shouldEnforceValue: true,
    isExclusive: true,
    ids: {
      refTypeId: RefTypeIds.ORDER_TYPE,
    },
    sideEffects: (state: FilterValues) => [
      {
        condition: isRefOrderType(state),
        result: [{ name: FilterIds.PART_FILLABLE, value: [ALL_VALUE] }],
      },
      {
        condition: isBothOrderType(state) || isLimitOrderType(state),
        result: [
          { name: FilterIds.REF_SOURCE, value: [ALL_VALUE] },
          { name: FilterIds.FX_INDEX, value: [ALL_VALUE] },
          { name: FilterIds.CURRENCY, value: [ALL_VALUE] },
          { name: FilterIds.PART_FILLABLE, value: [ALL_VALUE] },
          {
            name: FilterIds.CONTRACT_TEMPLATE,
            value: [
              RefIds.CONTRACT_TEMPLATE_AVERAGE,
              RefIds.CONTRACT_TEMPLATE_FIXED,
              RefIds.CONTRACT_TEMPLATE_FIXING,
              RefIds.CONTRACT_TEMPLATE_RELATIVE,
            ],
          },
        ],
      },
      {
        condition: isBothOrderType(state),
        result: [
          { name: FilterIds.OPEN_ORDER_STATUS, value: doPendingToBoth(state) },
        ],
      },
      {
        condition: isLimitOrderType(state) && isStatusReferencePending(state),
        result: [
          { name: FilterIds.OPEN_ORDER_STATUS, value: doPendingToLimit(state) },
        ],
      },
      {
        condition: isRefOrderType(state) && isStatusLimitPending(state),
        result: [
          {
            name: FilterIds.OPEN_ORDER_STATUS,
            value: doPendingToReference(state),
          },
        ],
      },
      {
        condition: isBothOrderType(state),
        result: [
          { name: FilterIds.CLOSED_ORDER_STATUS, callback: doPulledToBoth },
        ],
      },
      {
        condition: isLimitOrderType(state) && isStatusReferencePulled(state),
        result: [
          {
            name: FilterIds.CLOSED_ORDER_STATUS,
            callback: doPulledToLimit,
          },
        ],
      },
      {
        condition: isRefOrderType(state) && isStatusLimitPulled(state),
        result: [
          {
            name: FilterIds.CLOSED_ORDER_STATUS,
            callback: doPulledToReference,
          },
        ],
      },
      {
        condition: isBothOrderType(state),
        result: [
          {
            name: FilterIds.CLOSED_ORDER_STATUS,
            callback: doRejectedToBoth,
          },
        ],
      },
      {
        condition: isLimitOrderType(state) && isStatusReferenceRejected(state),
        result: [
          {
            name: FilterIds.CLOSED_ORDER_STATUS,
            callback: doRejectedToLimit,
          },
        ],
      },
      {
        condition: isRefOrderType(state) && isStatusLimitRejected(state),
        result: [
          {
            name: FilterIds.CLOSED_ORDER_STATUS,
            callback: doRejectedToReference,
          },
        ],
      },
    ],
  },
  {
    name: FilterIds.SIDE,
    queryIds: [OrderQueryIds.SIDE],
    heading: 'Trade Side',
    type: FilterType.LOZENGE,
    allOptionLabel: 'Both',
    shouldEnforceValue: true,
    isExclusive: true,
    ids: {
      refTypeId: RefTypeIds.SIDE,
    },
  },
  {
    name: FilterIds.PRODUCT_TICKER,
    queryIds: [OrderQueryIds.TICKER],
    heading: 'Product Ticker',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    ids: {
      refTypeId: RefTypeIds.TICKER,
    },
    optionGroups: [
      {
        subheading: 'Most Popular',
        options: TICKER_POPULAR_OPTIONS,
      },
    ],
  },
  {
    name: FilterIds.REF_SOURCE,
    queryIds: [OrderQueryIds.LEG_REF_SOURCE],
    heading: 'Reference Source',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    ids: {
      refTypeId: RefTypeIds.REF_SOURCE,
    },
    sideEffects: (state: FilterValues, filter: Filter) => [
      {
        condition: !isAllRefSource(state, filter),
        result: [{ name: FilterIds.ORDER_TYPE, value: [RefIds.REF_ORDER] }],
      },
    ],
    rules: (state: FilterValues) => [
      {
        condition: isLimitOrderType(state),
        result: {
          isHidden: true,
        },
      },
      {
        condition: isBothOrderType(state) || isRefOrderType(state),
        result: {
          isHidden: false,
        },
      },
    ],
  },
  {
    name: FilterIds.CURRENCY,
    queryIds: [OrderQueryIds.TERM_CURRENCY],
    heading: 'Settlement Currency',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    ids: {
      refTypeId: RefTypeIds.CURRENCY,
    },
    sideEffects: (state: FilterValues, filter: Filter) => [
      {
        condition: !isAllCurrency(state, filter),
        result: [{ name: FilterIds.ORDER_TYPE, value: [RefIds.REF_ORDER] }],
      },
    ],
    rules: (state: FilterValues) => [
      {
        condition: isLimitOrderType(state),
        result: {
          isHidden: true,
        },
      },
      {
        condition: isBothOrderType(state) || isRefOrderType(state),
        result: {
          isHidden: false,
        },
      },
    ],
  },
  {
    name: FilterIds.OPEN_ORDER_STATUS,
    queryIds: [OrderQueryIds.ORDER_STATUS],
    heading: 'Open Orders',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    isCollapsable: true,
    isCollapsedByDefault: true,
    ids: {
      orderStatusCategoryId: OrderStatusCategoryIds.OPEN,
      orderTypeNameId: [OrderTypeNameIds.LIMIT, OrderTypeNameIds.REFERENCE],
    },
    rules: (state: FilterValues) => [
      {
        condition: isLimitOrderType(state),
        result: {
          ids: {
            orderStatusCategoryId: OrderStatusCategoryIds.OPEN,
            orderTypeNameId: [OrderTypeNameIds.LIMIT],
          },
        },
      },
      {
        condition: isRefOrderType(state),
        result: {
          ids: {
            orderStatusCategoryId: OrderStatusCategoryIds.OPEN,
            orderTypeNameId: [OrderTypeNameIds.REFERENCE],
          },
        },
      },
      {
        condition: isBothOrderType(state),
        result: {
          ids: {
            orderStatusCategoryId: OrderStatusCategoryIds.OPEN,
            orderTypeNameId: [
              OrderTypeNameIds.LIMIT,
              OrderTypeNameIds.REFERENCE,
            ],
          },
        },
      },
    ],
  },
  {
    name: FilterIds.CLOSED_ORDER_STATUS,
    queryIds: [OrderQueryIds.ORDER_STATUS],
    heading: 'Closed Orders',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    isCollapsable: true,
    isCollapsedByDefault: true,
    ids: {
      orderStatusCategoryId: OrderStatusCategoryIds.CLOSED,
      orderTypeNameId: [OrderTypeNameIds.LIMIT, OrderTypeNameIds.REFERENCE],
    },
    rules: (state: FilterValues) => [
      {
        condition: isLimitOrderType(state),
        result: {
          ids: {
            orderStatusCategoryId: OrderStatusCategoryIds.CLOSED,
            orderTypeNameId: [OrderTypeNameIds.LIMIT],
          },
        },
      },
      {
        condition: isRefOrderType(state),
        result: {
          ids: {
            orderStatusCategoryId: OrderStatusCategoryIds.CLOSED,
            orderTypeNameId: [OrderTypeNameIds.REFERENCE],
          },
        },
      },
      {
        condition: isBothOrderType(state),
        result: {
          ids: {
            orderStatusCategoryId: OrderStatusCategoryIds.CLOSED,
            orderTypeNameId: [
              OrderTypeNameIds.LIMIT,
              OrderTypeNameIds.REFERENCE,
            ],
          },
        },
      },
    ],
  },
  {
    name: FilterIds.METAL_LOCATION,
    queryIds: [OrderQueryIds.METAL_LOCATION],
    heading: 'Location',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    isCollapsable: true,
    isCollapsedByDefault: true,
    ids: {
      refTypeId: RefTypeIds.METAL_LOCATION,
    },
  },
  {
    name: FilterIds.SUBMITTERS,
    queryIds: [OrderQueryIds.CREATED_BY],
    heading: 'Submitters',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    isCollapsable: true,
    isCollapsedByDefault: true,
  },
  {
    name: FilterIds.MATURE_ORDERS,
    queryIds: [OrderQueryIds.ORDER_STATUS],
    heading: 'Mature Orders',
    type: FilterType.LOZENGE,
    shouldEnforceValue: true,
    isExclusive: true,
    defaultValue: [OffOnIds.OFF],
  },
  {
    name: FilterIds.METAL_FORM,
    queryIds: [OrderQueryIds.METAL_FORM],
    heading: 'Metal Form',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    ids: {
      refTypeId: RefTypeIds.METAL_FORM,
    },
  },
  {
    name: FilterIds.PART_FILLABLE,
    queryIds: [OrderQueryIds.PART_FILLABLE],
    heading: 'Part Fillable',
    type: FilterType.LOZENGE,
    allOptionLabel: 'All',
    shouldEnforceValue: true,
    isExclusive: true,
    ids: {
      refTypeId: RefTypeIds.YES_NO,
    },
    sideEffects: (state: FilterValues) => [
      {
        condition: !isPartFillable(state) && !isNotPartFillable(state),
        result: [{ name: FilterIds.ORDER_TYPE, value: [ALL_VALUE] }],
      },
      {
        condition: isPartFillable(state) || isNotPartFillable(state),
        result: [{ name: FilterIds.ORDER_TYPE, value: [RefIds.LIMIT_ORDER] }],
      },
    ],
    rules: (state: FilterValues) => [
      {
        condition: isRefOrderType(state),
        result: {
          isHidden: true,
        },
      },
      {
        condition: isBothOrderType(state) || isLimitOrderType(state),
        result: {
          isHidden: false,
        },
      },
    ],
  },
  {
    name: FilterIds.CONTRACT_TEMPLATE,
    queryIds: [OrderQueryIds.CONTRACT_TYPE],
    heading: 'Contract Template',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    ids: {
      refTypeId: [RefTypeIds.LIMIT_CONTRACT_TYPE, RefTypeIds.REF_CONTRACT_TYPE],
    },
    sideEffects: (state: FilterValues) => [
      {
        condition: isContractTemplateLimit(state),
        result: [{ name: FilterIds.ORDER_TYPE, value: [RefIds.LIMIT_ORDER] }],
      },
      {
        condition: isContractTemplateReference(state),
        result: [{ name: FilterIds.ORDER_TYPE, value: [RefIds.REF_ORDER] }],
      },
    ],
    rules: (state: FilterValues) => [
      {
        condition: isLimitOrderType(state),
        result: {
          ids: {
            refTypeId: [RefTypeIds.LIMIT_CONTRACT_TYPE],
          },
        },
      },
      {
        condition: isRefOrderType(state),
        result: {
          ids: {
            refTypeId: [RefTypeIds.REF_CONTRACT_TYPE],
          },
        },
      },
      {
        condition: isBothOrderType(state),
        result: {
          ids: {
            refTypeId: [
              RefTypeIds.LIMIT_CONTRACT_TYPE,
              RefTypeIds.REF_CONTRACT_TYPE,
            ],
          },
        },
      },
    ],
  },
  {
    name: FilterIds.METAL_UNIT,
    queryIds: [OrderQueryIds.METAL_UNIT],
    heading: 'Metal Unit',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    ids: {
      refTypeId: RefTypeIds.METAL_UNIT,
    },
  },
  {
    name: FilterIds.FX_INDEX,
    queryIds: [OrderQueryIds.FX_INDEX],
    heading: 'FX Index Ref Source',
    type: FilterType.LIST,
    allOptionLabel: 'Select All',
    ids: {
      refTypeId: RefTypeIds.FX_INDEX_REF_SOURCE,
    },
    sideEffects: (state: FilterValues, filter: Filter) => [
      {
        condition: !isAllFxIndexRefSource(state, filter),
        result: [{ name: FilterIds.ORDER_TYPE, value: [RefIds.REF_ORDER] }],
      },
    ],
    rules: (state: FilterValues) => [
      {
        condition: isLimitOrderType(state),
        result: {
          isHidden: true,
        },
      },
      {
        condition: isBothOrderType(state) || isRefOrderType(state),
        result: {
          isHidden: false,
        },
      },
    ],
  },
];
