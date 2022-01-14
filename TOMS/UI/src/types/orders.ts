import { Nullable } from './util';

export enum OrderType {
  limit = 'Limit Order',
  reference = 'Reference Order',
}

export enum OrderTypeAbbreviated {
  limit = 'Limit',
  reference = 'Ref',
}

export enum OrderSide {
  buy = 'Buy',
  sell = 'Sell',
}

export enum YesNo {
  yes = 'Yes',
  no = 'No',
}

export enum ValidationType {
  goodTillCancelled = 'Good Till Cancelled',
  expiryDate = 'Expiry Date',
}

export enum PriceType {
  spot = 'Spot',
  forward = 'Forward',
}

export enum ContractTerms {
  fixed = 'Fixed',
  relative = 'Relative',
}

type OrderCommonProperties = {
  id: number;
  idOrderType: Nullable<number>;
  displayStringOrderType: Nullable<OrderType>;
  version: number;
  idInternalBu: Nullable<number>;
  displayStringInternalBu: Nullable<string>;
  idExternalBu: Nullable<number>;
  displayStringExternalBu: Nullable<string>;
  idInternalLe: Nullable<number>;
  displayStringInternalLe: Nullable<string>;
  idExternalLe: Nullable<number>;
  displayStringExternalLe: Nullable<string>;
  idIntPortfolio: Nullable<number>;
  displayStringIntPortfolio: Nullable<string>;
  idExtPortfolio: Nullable<number>;
  displayStringExtPortfolio: Nullable<string>;
  idBuySell: number;
  displayStringBuySell: OrderSide;
  idBaseCurrency: number;
  displayStringBaseCurrency: Nullable<string>;
  baseQuantity: Nullable<number>;
  idBaseQuantityUnit: Nullable<number>;
  displayStringBaseQuantityUnit: Nullable<string>;
  idTermCurrency: number;
  displayStringTermCurrency: Nullable<string>;
  reference: Nullable<string>;
  idMetalForm: Nullable<number>;
  displayStringMetalForm: Nullable<string>;
  idMetalLocation: Nullable<number>;
  displayStringMetalLocation: Nullable<string>;
  idOrderStatus: number;
  displayStringOrderStatus: Nullable<string>;
  creditChecksIds: Nullable<number[]>;
  createdAt: string;
  idCreatedByUser: number;
  displayStringCreatedByUser: Nullable<string>;
  lastUpdate: string;
  idUpdatedByUser: number;
  displayStringUpdatedByUser: Nullable<string>;
  orderCommentIds: number[];
  fillIds: Nullable<number[]>;
  fillPercentage: number;
  idTicker: Nullable<number>;
  displayStringTicker: Nullable<string>;
  idContractType: number;
  displayStringContractType: Nullable<string>;
};

export type LimitOrder = OrderCommonProperties & {
  displayStringOrderType: OrderType.limit;
  settleDate: Nullable<string>;
  startDateConcrete: Nullable<string>;
  idStartDateSymbolic: Nullable<number>;
  displayStringStartDateSymbolic: Nullable<number>;
  limitPrice: number;
  idPriceType: Nullable<number>;
  displayStringPriceType: Nullable<string>;
  idYesNoPartFillable: Nullable<number>;
  displayStringPartFillable: Nullable<string>;
  idStopTriggerType: Nullable<number>;
  displayStringStopTriggerType: Nullable<string>;
  idCurrencyCrossMetal: Nullable<number>;
  displayStringCurrencyCrossMetal: Nullable<string>;
  idValidationType: Nullable<number>;
  displayStringValidationType: Nullable<string>;
  expiryDate: Nullable<string>;
  executionLikelihood: Nullable<number>;
};

export type ReferenceOrder = OrderCommonProperties & {
  displayStringOrderType: OrderType.reference;
  metalPriceSpread: number;
  fxRateSpread: number;
  contangoBackwardation: Nullable<number>;
  legIds: number[];
};

export type Order = LimitOrder | ReferenceOrder;

type OrderCommonCreation = Pick<
  OrderCommonProperties,
  | 'id'
  | 'version'
  | 'idOrderType'
  | 'idOrderStatus'
  | 'idInternalBu'
  | 'idExternalBu'
  | 'idBaseCurrency'
  | 'idTermCurrency'
  | 'idTicker'
  | 'idBaseQuantityUnit'
  | 'baseQuantity'
  | 'idContractType'
  | 'idMetalLocation'
  | 'idMetalForm'
  | 'fillPercentage'
>;

export type LimitOrderCreation = OrderCommonCreation &
  Pick<
    LimitOrder,
    | 'idPriceType'
    | 'startDateConcrete'
    | 'settleDate'
    | 'limitPrice'
    | 'idYesNoPartFillable'
    | 'idValidationType'
  >;

export type ReferenceOrderCreation = OrderCommonCreation &
  // TODO: Pick<> reference orders once we work on creating ref orders
  Partial<ReferenceOrder>;

export type OrderCreation = LimitOrderCreation | ReferenceOrderCreation;

export enum OrderQueryIds {
  MIN_CREATED_AT_DATE = 'minCreatedAtDate',
  MAX_CREATED_AT_DATE = 'maxCreatedAtDate',
  INTERNAL_BUSINESS_UNIT = 'idInternalBu',
  EXTERNAL_BUSINESS_UNIT = 'idExternalBu',
  ORDER_TYPE = 'idOrderType',
  SIDE = 'idBuySell',
  TICKER = 'idTicker',
  LEG_REF_SOURCE = 'idLegRefSource',
  TERM_CURRENCY = 'idTermCurrency',
  ORDER_STATUS = 'idOrderStatus',
  CREATED_BY = 'idCreatedByUser',
  METAL_LOCATION = 'idMetalLocation',
  METAL_FORM = 'idMetalForm',
  METAL_UNIT = 'idBaseQuantityUnit',
  PART_FILLABLE = 'idYesNoPartFillable',
  CONTRACT_TYPE = 'idContractType',
  FX_INDEX = 'idLegFxIndexRefSource',
}

export type OrderStatusResponse = {
  id: number;
  idOrderStatusName: number;
  idOrderTypeName: number;
  idOrderTypeCategory: number;
  sortColumn: number;
};

export type OrderStatus = OrderStatusResponse & {
  orderStatusName: string;
  orderTypeName: string;
  orderTypeCategory: string;
};

export type OrderComment = {
  id: number;
  commentText: string;
  createdAt: string;
  idCreatedByUser: number;
  displayStringCreatedByUser: Nullable<string>;
  lastUpdate: string;
  idUpdatedByUser: number;
  displayStringUpdatedByUser: Nullable<string>;
  idLifeCycle: number;
  idAssociatedAction: Nullable<number>;
};

export type OrderCommentCreation = Pick<
  OrderComment,
  | 'id'
  | 'commentText'
  | 'createdAt'
  | 'idCreatedByUser'
  | 'lastUpdate'
  | 'idUpdatedByUser'
  | 'idLifeCycle'
  | 'idAssociatedAction'
>;

export type OrderFill = {
  id: number;
  fillQuantity: number;
  fillPrice: number;
  idTrader: number;
  displayStringTrader: Nullable<string>;
  idTrade: Nullable<string>;
  idFillStatus: number;
  displayStringFillStatus: Nullable<string>;
  idUpdatedBy: number;
  displayStringUpdatedBy: Nullable<string>;
  lastUpdateDateTime: string;
  errorMessage: Nullable<string>;
};

export type OrderFillCreation = Pick<
  OrderFill,
  | 'id'
  | 'fillQuantity'
  | 'fillPrice'
  | 'idTrader'
  | 'idUpdatedBy'
  | 'lastUpdateDateTime'
  | 'idFillStatus'
>;
