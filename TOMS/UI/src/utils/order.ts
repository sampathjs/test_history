import { OrderType, OrderTypeAbbreviated } from 'types';

export const formatOrderType = (type: OrderType) => {
  if (type === OrderType.limit) {
    return OrderTypeAbbreviated.limit;
  }

  return OrderTypeAbbreviated.reference;
};
