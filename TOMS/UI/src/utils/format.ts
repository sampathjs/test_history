import { NO_VALUE } from 'constants/format';
import { Nullable } from 'types/util';

export const formatValue = <Type>(value?: Nullable<Type>) => {
  return value ?? NO_VALUE;
};

export const formatQuantity = (quantity?: Nullable<number>) => {
  return quantity ? String(Math.round(quantity)) : formatValue(quantity);
};

export const formatPrice = (price?: Nullable<number>) => {
  return price ? price.toFixed(2) : formatValue(price);
};

export const formatToNumber = (value?: Nullable<string>) => {
  return Number(value);
};
