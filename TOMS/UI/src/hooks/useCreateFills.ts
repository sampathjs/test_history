import { useMutation, useQueryClient } from 'react-query';

import { Order, OrderFill, OrderFillCreation } from 'types';
import { request } from 'utils';

import { getEndpointTypeForOrderType } from './helpers';
import { MutationKeys, QueryKeys } from './types';

type CreateFillsRequest = {
  fills: OrderFillCreation[];
  id: Order['id'];
  idOrderType: Order['idOrderType'];
};

const createFills = (data: CreateFillsRequest) => {
  const { fills, id, idOrderType } = data;
  const endpoint = getEndpointTypeForOrderType(idOrderType);

  const promisesToCreateFills = fills.map((fill) =>
    request<OrderFill['id']>(`${endpoint}/${id}/fill`, {
      method: 'POST',
      body: JSON.stringify(fill),
    })
  );

  return Promise.all(promisesToCreateFills);
};

export const useCreateFills = () => {
  const queryClient = useQueryClient();

  return useMutation(createFills, {
    mutationKey: MutationKeys.CREATE_FILLS,
    onSuccess: (_, { id }) => {
      queryClient.invalidateQueries([QueryKeys.ORDER, id]);
      queryClient.invalidateQueries([QueryKeys.ORDER_FILLS, id]);
      queryClient.invalidateQueries(QueryKeys.ORDERS);
    },
  });
};
