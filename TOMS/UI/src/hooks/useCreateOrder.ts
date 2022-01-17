import { useMutation } from 'react-query';

import { Order, OrderCreation, OrderTypeNameIds } from 'types';
import { request } from 'utils';

import { MutationKeys } from './types';

type CreateOrderRequest = {
  order: OrderCreation;
};

const createOrder = ({ order }: CreateOrderRequest) => {
  const isLimitOrder = order.idOrderType === OrderTypeNameIds.LIMIT;
  const endpoint = isLimitOrder ? 'limitOrder' : 'referenceOrder';

  return request<Order['id']>(endpoint, {
    method: 'POST',
    body: JSON.stringify(order),
  });
};

export const useCreateOrder = () =>
  useMutation(createOrder, {
    mutationKey: MutationKeys.CREATE_ORDER,
  });
