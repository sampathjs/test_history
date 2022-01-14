import { SortingRule } from 'react-table';
import useLocalStorage from 'react-use/lib/useLocalStorage';

import { Order } from 'types';

export const useTableSortBy = () => {
  const [sortBy = [], setSortBy] = useLocalStorage<SortingRule<Order>[]>(
    'orders.sort',
    []
  );

  return [sortBy, setSortBy] as const;
};
