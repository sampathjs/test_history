import { Nullable } from './util';

export type Reference = {
  id: RefIds;
  idType: RefTypeIds;
  name: string;
  displayName: Nullable<string>;
};

export type ReferenceType = {
  id: RefTypeIds;
  name: string;
};

export type RefData = Omit<Reference, 'idType'> & {
  type: ReferenceType | undefined;
};

export type RefDataMap = Record<number, RefData>;

export enum RefIds {
  // Order type
  LIMIT_ORDER = 13,
  REF_ORDER = 14,
  // Side
  BUY = 15,
  SELL = 16,
  // Yes/No
  YES = 97,
  NO = 98,
  // Reference contract templates
  CONTRACT_TEMPLATE_AVERAGE = 224,
  CONTRACT_TEMPLATE_FIXING = 225,
  // Limit contract templates
  CONTRACT_TEMPLATE_RELATIVE = 226,
  CONTRACT_TEMPLATE_FIXED = 227,
  // Price types
  PRICE_TYPE_SPOT = 105,
  PRICE_TYPE_FORWARD = 106,
  // Validation type
  VALIDATION_GOOD_TILL_CANCELLED = 184,
  VALIDATION_EXPIRY_DATE = 185,
  // Lifecycle status
  LIFECYCLE_AUTHORISATION_PENDING = 290,
  LIFECYCLE_AUTHORISED_ACTIVE = 291,
  LIFECYCLE_AUTHORISED_INACTIVE = 292,
  LIFECYCLE_DELETED = 293,
  // Comment actions
  ACTION_REJECT = 379,
  ACTION_PULL = 380,
  ACTION_CANCEL = 381,
}

export enum RefTypeIds {
  ORDER_TYPE = 2,
  ORDER_STATUS = 4,
  SIDE = 5,
  METAL_UNIT = 8,
  METAL_SYMBOL = 9, // TODO - check a) if this is correct b) if terminology is right
  CURRENCY = 10,
  YES_NO = 12,
  METAL_FORM = 22,
  METAL_LOCATION = 23,
  REF_SOURCE = 26,
  FX_INDEX_REF_SOURCE = 26,
  REF_CONTRACT_TYPE = 27,
  LIMIT_CONTRACT_TYPE = 28,
  TICKER = 30,
  LIFECYCLE_STATUS = 35,
}

export enum OffOnIds {
  OFF = 0,
  ON = 1,
}

export enum OrderSideIds {
  BUY = RefIds.BUY,
  SELL = RefIds.SELL,
}

export enum OrderStatusCategoryIds {
  OPEN = 229,
  CLOSED = 230,
}

export enum OrderTypeNameIds {
  LIMIT = RefIds.LIMIT_ORDER,
  REFERENCE = RefIds.REF_ORDER,
}

export enum OrderStatusIds {
  LIMIT_PENDING = 1,
  LIMIT_PULLED = 2,
  LIMIT_CONFIRMED = 3,
  LIMIT_FILLED = 4,
  LIMIT_CANCELLED = 5,
  LIMIT_REJECTED = 6,
  LIMIT_PARTIALLY_FILLED = 7,
  LIMIT_PARTIALLY_FILLED_CANCELLED = 8,
  LIMIT_PARTIALLY_FILLED_EXPIRED = 9,
  LIMIT_EXPIRED = 10,
  LIMIT_MATURED = 11,
  REFERENCE_PENDING = 100,
  REFERENCE_PULLED = 101,
  REFERENCE_CONFIRMED = 102,
  REFERENCE_REJECTED = 103,
  REFERENCE_MATURED = 105,
}

export enum FillStatusIds {
  FILL_STATUS_OPEN = 286,
  FILL_STATUS_COMPLETED = 287,
  FILL_STATUS_FAILED = 288,
  FILL_STATUS_TRANSITION = 289,
}
