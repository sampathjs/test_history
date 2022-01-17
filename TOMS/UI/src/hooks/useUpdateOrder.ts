import { useMutation, useQueryClient } from 'react-query';

import { Order, OrderTypeNameIds } from 'types';
import { request } from 'utils';

import { MutationKeys, QueryKeys } from './types';

const getUpdateEndpoint = (order: Order) => {
  return order.idOrderType === OrderTypeNameIds.LIMIT
    ? 'limitOrder'
    : 'referenceOrder';
};

const updateOrder = (order: Order) => {
  const endpoint = getUpdateEndpoint(order);

  return request(endpoint, {
    method: 'PUT',
    body: JSON.stringify(order),
  });
};

export const useUpdateOrder = () => {
  const queryClient = useQueryClient();

  return useMutation(updateOrder, {
    mutationKey: MutationKeys.UPDATE_ORDER,
    onSuccess: (_, order) => {
      queryClient.invalidateQueries([QueryKeys.ORDER, order.id]);
      queryClient.invalidateQueries(QueryKeys.ORDERS);
    },
  });
};
