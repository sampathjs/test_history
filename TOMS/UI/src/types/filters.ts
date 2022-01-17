import { OrderStatusCategoryIds, PartyTypeIds, RefTypeIds } from 'types';

export enum FilterIds {
  CLOSED_ORDER_STATUS = 'closedOrderStatus',
  CONTRACT_TEMPLATE = 'contractTemplate',
  CURRENCY = 'termCurrency',
  EXECUTED_AT = 'executedAt',
  EXTERNAL_BUSINESS_UNIT = 'externalBusinessUnit',
  FX_INDEX = 'fxIndex',
  INTERNAL_BUSINESS_UNIT = 'internalBusinessUnit',
  MATURE_ORDERS = 'matureOrders',
  METAL_FORM = 'metalForm',
  METAL_LOCATION = 'metalLocation',
  METAL_UNIT = 'metalUnit',
  OPEN_ORDER_STATUS = 'openOrderStatus',
  ORDER_TYPE = 'orderType',
  PART_FILLABLE = 'partFillable',
  PRODUCT_TICKER = 'productTicker',
  REF_SOURCE = 'refSource',
  SIDE = 'side',
  SUBMITTERS = 'submitters',
}

export enum FilterType {
  LIST = 'list',
  LOZENGE = 'lozenge',
}

type FilterRule = {
  condition: boolean;
  result: Partial<FilterConfig>;
};

type FilterSideEffect = {
  condition: boolean;
  result: {
    name: FilterIds;
    value?: FilterOptionValue[];
    callback?: (state: FilterValues) => FilterValue;
  }[];
};

type FilterCore = {
  name: FilterIds;
  queryIds: string[];
  heading: string;
  type: FilterType;
  isHidden?: boolean;
  rules?: (state: FilterValues) => FilterRule[];
  sideEffects?: (state: FilterValues, filter: Filter) => FilterSideEffect[];
};

type FilterBehaviourAttributes = {
  allOptionLabel?: string;
  isCollapsable?: boolean;
  isCollapsedByDefault?: boolean;
  isExclusive?: boolean;
  shouldEnforceValue?: boolean;
};

export type FilterConfig = FilterCore &
  FilterBehaviourAttributes & {
    options?: FilterOption[];
    value?: FilterValue;
    onChange?: (name: string, update: FilterValue) => void;
    defaultValue?: FilterValue;
    ids?: {
      partyTypeId?: PartyTypeIds;
      refTypeId?: RefTypeIds | RefTypeIds[];
      orderStatusCategoryId?: OrderStatusCategoryIds;
      orderTypeNameId?: number[];
    };
    optionGroups?: FilterOptionGroup[];
  };

export type Filter = FilterCore &
  FilterBehaviourAttributes & {
    options: FilterOption[];
    value: FilterValue;
    onChange: (name: string, update: FilterValue) => void;
    optionGroups?: FilterOptionGroup[];
  };

export enum FilterOptionType {
  DATE_PICKER_RANGE = 'date-picker-range',
}

export type FilterOption = {
  name: string;
  value: FilterOptionValue;
  type?: FilterOptionType;
};

export type FilterOptionGroup = {
  subheading: string;
  options: string[];
};

export type FilterOptionValue = number | string | (number | string)[];

export type FilterValue = FilterOptionValue[];
export type FilterValues = Record<string, FilterValue>;

export type FilterQueryPair = [string, FilterValue];
