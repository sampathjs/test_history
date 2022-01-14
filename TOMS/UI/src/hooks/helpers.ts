import { OrderTypeNameIds } from 'types';
import { Nullable } from 'types/util';

export const getEndpointTypeForOrderType = (orderType: Nullable<number>) => {
  return orderType === OrderTypeNameIds.LIMIT ? 'limitOrder' : 'referenceOrder';
};
