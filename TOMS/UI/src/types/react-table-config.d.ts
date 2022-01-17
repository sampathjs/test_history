import {
  ColumnInstance as ReactTableColumnInstance,
  HeaderGroup as ReactTableHeaderGroup,
  TableInstance as ReactTableInstance,
  TableOptions as ReactTableOptions,
  TableState as ReactTableState,
  UseColumnOrderInstanceProps,
  UseColumnOrderState,
  UseSortByColumnProps,
  UseSortByOptions,
} from 'react-table';

declare module 'react-table' {
  export interface TableState<D extends Record<string, unknown>>
    extends ReactTableState<D>,
      UseColumnOrderState<D>,
      UseSortByState<D> {}

  export interface TableInstance<D extends Record<string, unknown>>
    extends ReactTableInstance<D>,
      UseColumnOrderInstanceProps<D> {}

  export interface TableOptions<D extends Record<string, unknown>>
    extends ReactTableOptions<D>,
      UseSortByOptions<D> {}

  export interface HeaderGroup<D extends Record<string, unknown>>
    extends ReactTableHeaderGroup<D>,
      UseSortByColumnProps<D> {}

  export interface ColumnInstance<D extends Record<string, unknown>>
    extends ReactTableColumnInstance<D>,
      UseSortByColumnProps<D> {}
}
