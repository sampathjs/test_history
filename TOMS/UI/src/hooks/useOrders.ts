import isEqual from 'lodash/isEqual';
import last from 'lodash/last';
import unionBy from 'lodash/unionBy';
import uniqBy from 'lodash/uniqBy';
import { useCallback } from 'react';
import { InfiniteData, useInfiniteQuery } from 'react-query';

import {
  FilterQueryPair,
  Order,
  OrderQueryIds,
  RefIds,
  SortDirection,
} from 'types';
import { buildUrl, request } from 'utils';

import { OrdersResponse, QueryKeys } from './types';

const NUMBER_OF_ORDERS_PER_PAGE = 50;

export type OrderSortAttribute = {
  id: keyof Order;
  direction: SortDirection;
};

type Props = {
  sort?: OrderSortAttribute[];
  filters?: FilterQueryPair[];
};

const mapSortAttributesToSearchParams = (attributes: OrderSortAttribute[]) => {
  const attributesWithDefaultSort = unionBy(
    [...attributes, { id: 'id', direction: SortDirection.desc }],
    'id'
  );

  return attributesWithDefaultSort.map((attribute) => [
    'sort',
    `${attribute.id},${attribute.direction}`,
  ]);
};

const mapFilterValuesToSearchParams = (values: FilterQueryPair[]) =>
  values.map(([id, value]) => [
    id,
    Array.isArray(value) ? value.join(',') : String(value),
  ]);

const getOrderType = (filters: FilterQueryPair[]) => {
  const type = filters?.find(
    ([queryId]) => queryId === OrderQueryIds.ORDER_TYPE
  )?.[1];

  return {
    isLimitOrder: isEqual(type, [RefIds.LIMIT_ORDER]),
    isRefOrder: isEqual(type, [RefIds.REF_ORDER]),
  };
};

const useOrdersQuery = (props: Props) => {
  const { sort = [], filters = [] } = props;

  return useInfiniteQuery(
    [QueryKeys.ORDERS, ...sort, ...filters],
    ({ pageParam }) => {
      const params = new URLSearchParams([
        ...mapFilterValuesToSearchParams(filters),
        ...mapSortAttributesToSearchParams(sort),
        ['size', String(NUMBER_OF_ORDERS_PER_PAGE)],
        ['idFirstOrderIncluded', pageParam ?? ''],
      ]).toString();

      const { isLimitOrder, isRefOrder } = getOrderType(filters);

      const endpoint = isLimitOrder
        ? 'limitOrder'
        : isRefOrder
        ? 'referenceOrder'
        : 'order';

      return request<OrdersResponse>(buildUrl(endpoint, params));
    },
    {
      enabled: !!filters.length,
      getNextPageParam(lastPage) {
        return lastPage.content.length < NUMBER_OF_ORDERS_PER_PAGE
          ? undefined
          : last(lastPage.content)?.id;
      },
    }
  );
};

const getTotalNumberOfOrdersFromData = (
  data?: InfiniteData<OrdersResponse>
) => {
  return data?.pages[0].totalElements ?? 0;
};

const EMPTY_ORDERS_LIST: Order[] = [];

const getOrdersFromData = (data?: InfiniteData<OrdersResponse>) => {
  if (!data) {
    return EMPTY_ORDERS_LIST;
  }

  const allOrders = data.pages.flatMap((page) => page.content);

  return uniqBy(allOrders, 'id');
};

export const useOrders = (props: Props = {}) => {
  const {
    data,
    fetchNextPage,
    hasNextPage,
    isError,
    isFetchingNextPage,
    isLoading,
  } = useOrdersQuery(props);
  const canFetchMoreMessages = hasNextPage && !isFetchingNextPage;

  const loadMoreOrders = useCallback(() => {
    if (canFetchMoreMessages) {
      fetchNextPage();
    }
  }, [canFetchMoreMessages, fetchNextPage]);

  return {
    isError,
    isLoading,
    loadMoreOrders,
    orders: getOrdersFromData(data),
    totalNumberOfOrders: getTotalNumberOfOrdersFromData(data),
  };
};
