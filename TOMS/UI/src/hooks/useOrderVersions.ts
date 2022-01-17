import { useQuery } from 'react-query';

import { Order } from 'types/orders';
import { buildUrl, request } from 'utils';

import { OrdersResponse, QueryKeys } from './types';

export const useOrderVersions = (
  id: Order['id'],
  versionIds?: Array<Order['version']>
) => {
  return useQuery(
    [QueryKeys.ORDER_VERSIONS, id, versionIds],
    async () => {
      const params = new URLSearchParams([['orderIds', String(id)]]);

      if (versionIds) {
        versionIds.map((versionId) =>
          params.append('versionIds', String(versionId))
        );
      }

      const data = await request<OrdersResponse>(
        buildUrl('order', params.toString())
      );

      return data.content;
    },
    {
      enabled: Boolean(id) && id !== -1,
    }
  );
};
