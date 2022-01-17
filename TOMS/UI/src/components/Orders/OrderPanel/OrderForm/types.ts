import { OptionType } from 'components/Select';
import {
  ContractTerms,
  OrderSide,
  OrderType,
  PriceType,
  ValidationType,
  YesNo,
} from 'types';
import { Nullable } from 'types/util';

export type RawFormInputs = {
  orderSide: Nullable<OrderSide>;
  orderType: Nullable<OrderType>;
  orderRef: Nullable<string>;
  jmPmmUnit: Nullable<OptionType>;
  counterparty: Nullable<OptionType>;
  productTicker: Nullable<OptionType>;
  metalUnit: Nullable<OptionType>;
  metalQty: string;
  priceType: Nullable<PriceType>;
  contractTemplate: Nullable<ContractTerms>;
  limitPrice: string;
  startDate: Date;
  settleDate: Nullable<Date>;
  validationType: Nullable<ValidationType>;
  partFillable: Nullable<YesNo>;
  location: Nullable<OptionType>;
  metalForm: Nullable<OptionType>;
  comment: string;
};

export type ParsedFormInputs = {
  orderSide: OrderSide;
  orderType: OrderType;
  jmPmmUnit: OptionType;
  counterparty: OptionType;
  productTicker: OptionType;
  metalUnit: OptionType;
  metalQty: string;
  priceType: PriceType;
  contractTemplate: ContractTerms;
  limitPrice: string;
  startDate: Date;
  settleDate: Date;
  validationType: ValidationType;
  partFillable: YesNo;
  location: OptionType;
  metalForm: OptionType;
  orderRef: string;
  comment: string;
};
