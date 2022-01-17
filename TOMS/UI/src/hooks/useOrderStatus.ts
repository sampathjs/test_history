import keyBy from 'lodash/keyBy';
import sortBy from 'lodash/sortBy';
import { useQueries } from 'react-query';

import { OrderStatus, OrderStatusResponse, Reference } from 'types';
import { Nullable } from 'types/util';
import { buildUrl, request } from 'utils';

import { QueryKeys } from './types';

type OrderStatusParams = {
  orderStatusId?: Nullable<number>;
  orderTypeNameId?: Nullable<number>;
};

const useOrderStatusQuery = (orderStatusParams?: OrderStatusParams) => {
  const params = orderStatusParams
    ? new URLSearchParams(
        Object.entries(orderStatusParams).map(([id, value]) => [
          id,
          String(value),
        ])
      ).toString()
    : '';

  return useQueries([
    {
      queryKey: [QueryKeys.ORDER_STATUS],
      queryFn: () =>
        request<OrderStatusResponse[]>(buildUrl('orderStatus', params)),
    },
    {
      queryKey: QueryKeys.REFERENCE_DATA,
      queryFn: () => request<Reference[]>('references'),
    },
  ]);
};

export const useOrderStatus = (orderStatusParams?: OrderStatusParams) => {
  const results = useOrderStatusQuery(orderStatusParams);
  const [{ data: orderStatuses }, { data: references }] = results;

  const refData = keyBy(references, 'id');
  const mappedData: OrderStatus[] | undefined = orderStatuses?.map((status) => {
    return {
      ...status,
      orderStatusName:
        refData[status.idOrderStatusName]?.displayName ??
        refData[status.idOrderStatusName]?.name,
      orderTypeName: refData[status.idOrderTypeName]?.name,
      orderTypeCategory:
        refData[status.idOrderTypeCategory]?.displayName ??
        refData[status.idOrderTypeCategory]?.name,
    };
  });

  return {
    isLoading: results.some((result) => result.isLoading),
    isSuccess: results.every((result) => result.isSuccess),
    data: mappedData ? sortBy(mappedData, 'id') : undefined,
  };
};
