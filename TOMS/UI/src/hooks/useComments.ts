import { useQuery } from 'react-query';

import { Order, OrderComment } from 'types';
import { request } from 'utils';

import { getEndpointTypeForOrderType } from './helpers';
import { QueryKeys } from './types';

export const useComments = (order?: Order) => {
  return useQuery(
    [QueryKeys.COMMENTS, order?.id],
    () => {
      if (!order) {
        return;
      }

      const endpoint = `${getEndpointTypeForOrderType(order.idOrderType)}/${
        order.id
      }/comments/`;

      return request<OrderComment[]>(endpoint);
    },
    { enabled: Boolean(order) }
  );
};
