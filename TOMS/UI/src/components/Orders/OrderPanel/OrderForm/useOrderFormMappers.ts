import { useCallback } from 'react';

import { useParties } from 'hooks/useParties';
import { useRefData } from 'hooks/useRefData';
import { usePortfolioTickerRules } from 'hooks/useTickerPortfolioRules';
import {
  KeyedPortfolioTickerRule,
  LimitOrder,
  Order,
  OrderType,
  Party,
  RefData,
  RefDataMap,
} from 'types';
import { Nullable } from 'types/util';
import { formatToApiDate, parseApiDate } from 'utils/date';
import {
  getValueFromSelectedOption,
  makeSelectOptionFromPartyData,
  makeSelectOptionFromRefData,
} from 'utils/form';
import { formatToNumber } from 'utils/format';

import {
  getBaseAndTermCcysFromProductTicker,
  getIdForContractType,
  getIdForOrderSide,
  getIdForOrderType,
  getIdForPartFillable,
  getIdForPortfolio,
  getIdForPriceType,
  getIdForValidationType,
} from '../helpers';
import {
  getContractTypeForRefId,
  getCurrencyByNameFromRefData,
  getMetalFormFromRefData,
  getMetalLocationFromRefData,
  getMetalSymbolByNameFromRefData,
  getMetalUnitFromRefData,
  getPartFillableForRefId,
  getParty,
  getPriceTypeForRefId,
  getProductTickerFromRefData,
  getValidationTypeForRefId,
} from './helpers';
import { ParsedFormInputs, RawFormInputs } from './types';

const mapPartyToSelectOption = (party?: Party) => {
  return party && makeSelectOptionFromPartyData(party);
};

const mapRefDataToSelectOption = (data?: RefData) => {
  return data && makeSelectOptionFromRefData(data);
};

const mapCommonOrderPropertiesToFormValues = (
  order: Order,
  parties?: Party[],
  refData?: RefDataMap
) => {
  const jmPmmUnit = getParty(order.idInternalBu, parties);
  const counterparty = getParty(order.idExternalBu, parties);

  const productTicker = getProductTickerFromRefData(order.idTicker, refData);
  const metalUnit = getMetalUnitFromRefData(order.idBaseQuantityUnit, refData);
  const location = getMetalLocationFromRefData(order.idMetalLocation, refData);
  const metalForm = getMetalFormFromRefData(order.idMetalForm, refData);

  return {
    orderSide: order.displayStringBuySell,
    orderType: order.displayStringOrderType,
    orderRef: order.reference,
    jmPmmUnit: mapPartyToSelectOption(jmPmmUnit),
    counterparty: mapPartyToSelectOption(counterparty),
    productTicker: mapRefDataToSelectOption(productTicker),
    metalUnit: mapRefDataToSelectOption(metalUnit),
    metalQty: String(order.baseQuantity),
    location: mapRefDataToSelectOption(location),
    metalForm: mapRefDataToSelectOption(metalForm),
  };
};

const mapLimitOrderPropertiesToFormValues = (order: LimitOrder) => {
  return {
    priceType: getPriceTypeForRefId(order.idPriceType),
    contractTemplate: getContractTypeForRefId(order.idContractType),
    limitPrice: String(order.limitPrice),
    startDate: parseApiDate(order.startDateConcrete)?.toJSDate(),
    settleDate: parseApiDate(order.settleDate)?.toJSDate(),
    validationType: getValidationTypeForRefId(order.idValidationType),
    partFillable: getPartFillableForRefId(order.idYesNoPartFillable),
  };
};

const mapFormValuesToLimitOrderProperties = (
  values: ParsedFormInputs,
  refData?: RefDataMap,
  portfolioTickerRules?: Record<string, KeyedPortfolioTickerRule>
) => {
  const [baseCcy, termCcy] = getBaseAndTermCcysFromProductTicker(
    values.productTicker
  );
  const baseCurrency = getMetalSymbolByNameFromRefData(baseCcy, refData);
  const termCurrency = getCurrencyByNameFromRefData(termCcy, refData);

  const idInternalBu = getValueFromSelectedOption(
    values.jmPmmUnit
  ) as Nullable<number>;
  const idTicker = getValueFromSelectedOption(
    values.productTicker
  ) as Nullable<number>;

  const idIntPortfolio = getIdForPortfolio(
    portfolioTickerRules,
    idInternalBu,
    idTicker
  );

  return {
    idBuySell: getIdForOrderSide(values.orderSide),
    idOrderType: getIdForOrderType(values.orderType),
    idInternalBu,
    idExternalBu: getValueFromSelectedOption(
      values.counterparty
    ) as Nullable<number>,
    idBaseCurrency: baseCurrency?.id,
    idTermCurrency: termCurrency?.id,
    idTicker,
    idBaseQuantityUnit: getValueFromSelectedOption(
      values.metalUnit
    ) as Nullable<number>,
    baseQuantity: formatToNumber(values.metalQty),
    idPriceType: getIdForPriceType(values.priceType),
    idContractType: getIdForContractType(values.contractTemplate),
    limitPrice: formatToNumber(values.limitPrice),
    startDateConcrete: formatToApiDate(values.startDate),
    settleDate: formatToApiDate(values.settleDate),
    idValidationType: getIdForValidationType(values.validationType),
    idYesNoPartFillable: getIdForPartFillable(values.partFillable),
    idMetalLocation: getValueFromSelectedOption(
      values.location
    ) as Nullable<number>,
    idMetalForm: getValueFromSelectedOption(
      values.metalForm
    ) as Nullable<number>,
    reference: values.orderRef,
    idIntPortfolio,
  };
};

export const useOrderFormMappers = () => {
  const { data: parties } = useParties();
  const { data: portfolioTickerRules } = usePortfolioTickerRules();
  const refData = useRefData();

  const mapOrderToFormValues = useCallback(
    (order: Order): Partial<RawFormInputs> => {
      const values = mapCommonOrderPropertiesToFormValues(
        order,
        parties,
        refData
      );

      if (order.displayStringOrderType === OrderType.reference) {
        return values;
      }

      return {
        ...values,
        ...mapLimitOrderPropertiesToFormValues(order),
      };
    },
    [refData, parties]
  );

  const mapFormValuesToOrder = (values: ParsedFormInputs) => {
    return mapFormValuesToLimitOrderProperties(
      values,
      refData,
      portfolioTickerRules
    );
  };

  return {
    mapOrderToFormValues,
    mapFormValuesToOrder,
  };
};
