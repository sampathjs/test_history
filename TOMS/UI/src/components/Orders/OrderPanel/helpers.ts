import orderBy from 'lodash/orderBy';

import { OptionGroup, OptionType } from 'components/Select';
import { TICKER_POPULAR_OPTIONS } from 'contexts/Filters/config';
import { getPartyDataByTypeId } from 'contexts/Filters/helpers';
import {
  ContractTerms,
  KeyedPortfolioTickerRule,
  OrderComment,
  OrderCommentCreation,
  OrderSide,
  OrderStatusIds,
  OrderType,
  OrderTypeNameIds,
  Party,
  PartyTypeIds,
  PriceType,
  RefData,
  RefIds,
  User,
  ValidationType,
  YesNo,
} from 'types';
import { Nullable } from 'types/util';
import { createApiDate, getDateToday } from 'utils/date';
import { getSelectOptionsFromRefData } from 'utils/form';

export const getProductTickerOptions = (
  productTickers: RefData[]
): OptionGroup[] => [
  {
    label: 'Most Used',
    options: TICKER_POPULAR_OPTIONS.filter(
      (ticker: string) =>
        productTickers?.find(({ name }) => name === ticker)?.name
    ).map((ticker: string) => ({
      label: ticker,
      value: productTickers?.find(({ name }) => name === ticker)?.id || null,
    })),
  },
  {
    label: 'Others',
    options: getSelectOptionsFromRefData(productTickers),
  },
];

export const getTradeableParties = (user: User, partyData?: Party[]) => {
  const internalParties = getPartyDataByTypeId(
    partyData,
    PartyTypeIds.INTERNAL_BUSINESS_UNIT
  ).filter(({ id }) => user.tradeableInternalPartyIds.includes(id));

  const externalParties = getPartyDataByTypeId(
    partyData,
    PartyTypeIds.EXTERNAL_BUSINESS_UNIT
  ).filter(({ id }) => user.tradeableCounterPartyIds.includes(id));

  return [internalParties, externalParties];
};

export const getIdForOrderSide = (side: OrderSide) =>
  side === OrderSide.buy ? RefIds.BUY : RefIds.SELL;

export const getIdForOrderType = (type: OrderType) =>
  type === OrderType.limit ? RefIds.LIMIT_ORDER : RefIds.REF_ORDER;

export const getIdForOrderStatusPending = (type: OrderType) =>
  type === OrderType.limit
    ? OrderStatusIds.LIMIT_PENDING
    : OrderStatusIds.REFERENCE_PENDING;

export const getIdForPartFillable = (isPartFillable: YesNo) =>
  isPartFillable === YesNo.yes ? RefIds.YES : RefIds.NO;

export const getIdForContractType = (term: ContractTerms) =>
  term === ContractTerms.fixed
    ? RefIds.CONTRACT_TEMPLATE_FIXED
    : RefIds.CONTRACT_TEMPLATE_RELATIVE;

export const getIdForPriceType = (type: PriceType) =>
  type === PriceType.spot ? RefIds.PRICE_TYPE_SPOT : RefIds.PRICE_TYPE_FORWARD;

export const getIdForValidationType = (type: ValidationType) =>
  type === ValidationType.goodTillCancelled
    ? RefIds.VALIDATION_GOOD_TILL_CANCELLED
    : RefIds.VALIDATION_EXPIRY_DATE;

export const getIdForPortfolio = (
  groupedPortfolioTickerRules?: Record<string, KeyedPortfolioTickerRule>,
  idInternalBu?: Nullable<number>,
  idTicker?: Nullable<number>
) => {
  if (!idInternalBu || !idTicker) {
    return null;
  }

  return (
    groupedPortfolioTickerRules?.[idInternalBu]?.[idTicker]?.idPortfolio || null
  );
};

export const getBaseAndTermCcysFromProductTicker = (
  productTicker: OptionType
) => {
  // TODO - we might need an option type that doesn't return null
  // So we can avoid this check
  if (!productTicker.value) {
    return [undefined, undefined];
  }
  const splitValue = productTicker.label.toString().split('/');
  const baseCcy = splitValue[0];
  const termCcy = splitValue[1];

  return [baseCcy, termCcy];
};

export const isPendingStatus = (status: OrderStatusIds) => {
  return (
    status === OrderStatusIds.LIMIT_PENDING ||
    status === OrderStatusIds.REFERENCE_PENDING
  );
};

export const isRejectedStatus = (status: OrderStatusIds) => {
  return (
    status === OrderStatusIds.LIMIT_REJECTED ||
    status === OrderStatusIds.REFERENCE_REJECTED
  );
};

export const isPulledStatus = (status: OrderStatusIds) => {
  return (
    status === OrderStatusIds.LIMIT_PULLED ||
    status === OrderStatusIds.REFERENCE_PULLED
  );
};

export const isConfirmedStatus = (status: OrderStatusIds) => {
  return (
    status === OrderStatusIds.LIMIT_CONFIRMED ||
    status === OrderStatusIds.REFERENCE_CONFIRMED
  );
};

export const isCancelledStatus = (status: OrderStatusIds) => {
  return (
    status === OrderStatusIds.LIMIT_CANCELLED ||
    status === OrderStatusIds.LIMIT_PARTIALLY_FILLED_CANCELLED
  );
};

export const isPartiallyFilledStatus = (status: OrderStatusIds) => {
  return status === OrderStatusIds.LIMIT_PARTIALLY_FILLED;
};

export const getRejectedStatusForOrderType = (
  type: Nullable<OrderTypeNameIds>
) => {
  return type === OrderTypeNameIds.LIMIT
    ? OrderStatusIds.LIMIT_REJECTED
    : OrderStatusIds.REFERENCE_REJECTED;
};

export const getPulledStatusForOrderType = (
  type: Nullable<OrderTypeNameIds>
) => {
  return type === OrderTypeNameIds.LIMIT
    ? OrderStatusIds.LIMIT_PULLED
    : OrderStatusIds.REFERENCE_PULLED;
};

export const getConfirmedStatusForOrderType = (
  type: Nullable<OrderTypeNameIds>
) => {
  return type === OrderTypeNameIds.LIMIT
    ? OrderStatusIds.LIMIT_CONFIRMED
    : OrderStatusIds.REFERENCE_CONFIRMED;
};

export const getCancelledStatusForOrderStatus = (status: OrderStatusIds) => {
  return isPartiallyFilledStatus(status)
    ? OrderStatusIds.LIMIT_PARTIALLY_FILLED_CANCELLED
    : OrderStatusIds.LIMIT_CANCELLED;
};

export const getExpiredStatusForOrderStatus = (status: OrderStatusIds) => {
  return isPartiallyFilledStatus(status)
    ? OrderStatusIds.LIMIT_PARTIALLY_FILLED_EXPIRED
    : OrderStatusIds.LIMIT_EXPIRED;
};

export const buildNewComment = (
  comment: string,
  userId: User['id']
): OrderCommentCreation => {
  const date = createApiDate();

  return {
    id: 0,
    createdAt: date,
    lastUpdate: date,
    idCreatedByUser: userId,
    idUpdatedByUser: userId,
    commentText: comment,
    idLifeCycle: RefIds.LIFECYCLE_AUTHORISED_ACTIVE,
    idAssociatedAction: null,
  };
};

export const buildNewCommentWithAction = (
  comment: string,
  userId: User['id'],
  status: OrderStatusIds
): OrderCommentCreation => {
  return {
    ...buildNewComment(comment, userId),
    idAssociatedAction: getCommentActionForStatus(status),
  };
};

export const getCommentActionForStatus = (status: OrderStatusIds) => {
  if (isRejectedStatus(status)) {
    return RefIds.ACTION_REJECT;
  }

  if (isPulledStatus(status)) {
    return RefIds.ACTION_PULL;
  }

  if (isCancelledStatus(status)) {
    return RefIds.ACTION_CANCEL;
  }

  return null;
};

export const getLatestNonActionComment = (comments: OrderComment[] = []) => {
  return orderBy(comments, ['id'], ['desc']).filter(
    (comment) => comment.idAssociatedAction === null
  )[0];
};

export const getLatestCommentForStatus = (
  comments: OrderComment[] = [],
  status: OrderStatusIds
) => {
  return orderBy(comments, ['id'], ['desc']).filter(
    (comment) =>
      comment.idAssociatedAction !== null &&
      comment.idAssociatedAction === getCommentActionForStatus(status)
  )[0];
};

export const getPlaceholderLabelForStatus = (status: OrderStatusIds) => {
  if (isRejectedStatus(status)) {
    return 'rejecting';
  }

  if (isPulledStatus(status)) {
    return 'pulling';
  }

  if (isCancelledStatus(status)) {
    return 'cancelling';
  }
};

export const getActionLabelForStatus = (status: OrderStatusIds) => {
  if (isRejectedStatus(status)) {
    return 'reject';
  }

  if (isPulledStatus(status)) {
    return 'pull';
  }

  if (isCancelledStatus(status)) {
    return 'cancel';
  }
};

export const getDefaultFormValues = () => ({
  orderSide: OrderSide.buy,
  orderType: OrderType.limit,
  orderRef: '',
  jmPmmUnit: null,
  counterparty: null,
  productTicker: null,
  metalUnit: null,
  metalQty: '',
  priceType: PriceType.spot,
  contractTemplate: ContractTerms.fixed,
  limitPrice: '',
  startDate: getDateToday().toJSDate(),
  settleDate: getDateToday().toJSDate(),
  validationType: ValidationType.goodTillCancelled,
  partFillable: YesNo.no,
  location: null,
  metalForm: null,
  comment: '',
});
