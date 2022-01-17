import { useQuery } from 'react-query';

import { Order, OrderFill, OrderTypeNameIds } from 'types';
import { buildUrl, request } from 'utils';

import { QueryKeys } from './types';

const getOrderFills = (order: Order) => {
  const isLimitOrder = order.idOrderType === OrderTypeNameIds.LIMIT;
  const endpoint = isLimitOrder ? 'limitOrder' : 'referenceOrder';

  return request<OrderFill[]>(buildUrl(`${endpoint}/${order.id}/fill`));
};

export const useOrderFills = (order: Order) => {
  return useQuery([QueryKeys.ORDER_FILLS, order.id], () =>
    getOrderFills(order)
  );
};
