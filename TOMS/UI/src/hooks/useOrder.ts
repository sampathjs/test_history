import { useQuery } from 'react-query';

import { Order } from 'types/orders';
import { buildUrl, request } from 'utils';

import { OrdersResponse, QueryKeys } from './types';

export const useOrder = (id: Order['id'], versionId?: Order['version']) => {
  return useQuery(
    [QueryKeys.ORDER, id, versionId],
    async () => {
      const params = new URLSearchParams([['orderIds', String(id)]]);

      if (versionId) {
        params.append('versionIds', String(versionId));
      }

      const data = await request<OrdersResponse>(
        buildUrl('order', params.toString())
      );

      return data.content[0];
    },
    {
      enabled: Boolean(id) && id !== -1,
    }
  );
};
