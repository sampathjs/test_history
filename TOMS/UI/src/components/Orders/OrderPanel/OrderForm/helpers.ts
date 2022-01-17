import isNull from 'lodash/isNull';

import {
  ContractTerms,
  Party,
  PriceType,
  RefDataMap,
  RefIds,
  RefTypeIds,
  ValidationType,
  YesNo,
} from 'types';
import { Nullable } from 'types/util';
import { getRefDataByTypeId } from 'utils/references';

export const getParty = (partyId: Nullable<Party['id']>, parties?: Party[]) => {
  return parties?.find((party) => party.id === partyId);
};

const getRefDataByTypeAndId = (
  data?: RefDataMap,
  type?: RefTypeIds,
  id?: Nullable<number>
) => {
  return getRefDataByTypeId(data, type).find(
    (dataForType) => dataForType.id === id
  );
};

const getRefDataByTypeAndName = (
  data?: RefDataMap,
  type?: RefTypeIds,
  name?: string
) => {
  return getRefDataByTypeId(data, type).find(
    (dataForType) => dataForType.name === name
  );
};

export const getProductTickerFromRefData = (
  id: Nullable<number>,
  data?: RefDataMap
) => {
  return getRefDataByTypeAndId(data, RefTypeIds.TICKER, id);
};

export const getMetalUnitFromRefData = (
  id: Nullable<number>,
  data?: RefDataMap
) => {
  return getRefDataByTypeAndId(data, RefTypeIds.METAL_UNIT, id);
};

export const getMetalLocationFromRefData = (
  id: Nullable<number>,
  data?: RefDataMap
) => {
  return getRefDataByTypeAndId(data, RefTypeIds.METAL_LOCATION, id);
};

export const getMetalFormFromRefData = (
  id: Nullable<number>,
  data?: RefDataMap
) => {
  return getRefDataByTypeAndId(data, RefTypeIds.METAL_FORM, id);
};

export const getMetalSymbolByNameFromRefData = (
  name?: string,
  data?: RefDataMap
) => {
  return getRefDataByTypeAndName(data, RefTypeIds.METAL_SYMBOL, name);
};

export const getCurrencyByNameFromRefData = (
  name?: string,
  data?: RefDataMap
) => {
  return getRefDataByTypeAndName(data, RefTypeIds.CURRENCY, name);
};

export const getPriceTypeForRefId = (refId: Nullable<RefIds>) => {
  if (isNull(refId)) {
    return null;
  }

  return refId === RefIds.PRICE_TYPE_SPOT ? PriceType.spot : PriceType.forward;
};

export const getContractTypeForRefId = (refId: Nullable<RefIds>) => {
  if (isNull(refId)) {
    return null;
  }

  return refId === RefIds.CONTRACT_TEMPLATE_FIXED
    ? ContractTerms.fixed
    : ContractTerms.relative;
};

export const getValidationTypeForRefId = (refId: Nullable<RefIds>) => {
  if (isNull(refId)) {
    return null;
  }

  return refId === RefIds.VALIDATION_GOOD_TILL_CANCELLED
    ? ValidationType.goodTillCancelled
    : ValidationType.expiryDate;
};

export const getPartFillableForRefId = (refId: Nullable<RefIds>) => {
  if (isNull(refId)) {
    return null;
  }

  return refId === RefIds.YES ? YesNo.yes : YesNo.no;
};
