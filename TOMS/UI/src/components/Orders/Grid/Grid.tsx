import { useEffect, useMemo, useRef, useState } from 'react';
import {
  useColumnOrder,
  useFlexLayout,
  useSortBy,
  useTable,
} from 'react-table';
import useUpdateEffect from 'react-use/lib/useUpdateEffect';

import { ChevronDownIcon, ChevronUpIcon } from 'components/Icon';
import {
  TableCell,
  TableHeaderCell,
  TableHeaderCellSortIcon,
  TableHeaderCellTitle,
  TableHeaderRow,
  TableNoResults,
} from 'components/Table';
import {
  OrderPanelViewType,
  useFilterContext,
  useOrderPanelContext,
  useTimeZoneContext,
} from 'contexts';
import { useOrders } from 'hooks/useOrders';
import { Order } from 'types';
import { Nullable } from 'types/util';

import { mapAllSelectedToEmptyArray } from '../Filters/helpers';
import { ColumnPicker } from './ColumnPicker';
import * as Styles from './Grid.styles';
import { GridCellValue } from './GridCellValue';
import { Header } from './Header';
import { columns, mapSortingRulesToSortAttributes } from './helpers';
import { RowSelector } from './RowSelector';
import { useElementScrollbarWidth } from './useElementScrollbarWidth';
import { useInfiniteVirtual } from './useInfiniteVirtual';
import { useSelectedColumns } from './useSelectedColumns';
import { useTableSortBy } from './useTableSortBy';

export const Grid = () => {
  const [isColumnPickerVisible, setIsColumnPickerVisible] = useState(false);
  const { filters, state: filterState } = useFilterContext();
  const [sortBy, setSortBy] = useTableSortBy();
  const { isError, isLoading, loadMoreOrders, orders, totalNumberOfOrders } =
    useOrders({
      filters: mapAllSelectedToEmptyArray(filters, filterState),
      sort: mapSortingRulesToSortAttributes(sortBy),
    });

  const hasOrders = Boolean(orders.length);
  const {
    resetSelectedColumns,
    saveSelectedColumns,
    selectedColumns,
    setSelectedColumns,
  } = useSelectedColumns();
  const { timeZone } = useTimeZoneContext();
  const bodyRef = useRef<Nullable<HTMLDivElement>>(null);
  const bodyScrollbarWidth = useElementScrollbarWidth(bodyRef);
  const [hoveredColumn, setHoveredColumn] = useState('');
  const { setView, view } = useOrderPanelContext();

  const hiddenColumns = useMemo(() => {
    return selectedColumns
      .filter((column) => !column.isVisible)
      .map((column) => String(column.id));
  }, [selectedColumns]);

  const columnOrder = useMemo(() => {
    return selectedColumns.map((column) => String(column.id));
  }, [selectedColumns]);

  const {
    getTableBodyProps,
    getTableProps,
    headerGroups,
    prepareRow,
    rows,
    setColumnOrder,
    setHiddenColumns,
    state: { sortBy: currentSortBy },
  } = useTable(
    {
      columns,
      data: orders,
      initialState: {
        hiddenColumns,
        columnOrder,
        sortBy,
      },
      manualSortBy: true,
    },
    useFlexLayout,
    useColumnOrder,
    useSortBy
  );

  const { shouldLoadMore, totalSize, virtualItems } = useInfiniteVirtual({
    parentRef: bodyRef,
    size: rows.length,
    overscan: 5,
  });

  useEffect(() => {
    if (shouldLoadMore) {
      loadMoreOrders();
    }
  }, [shouldLoadMore, loadMoreOrders]);

  useEffect(() => {
    setHiddenColumns(hiddenColumns);
  }, [setHiddenColumns, hiddenColumns]);

  useEffect(() => {
    setColumnOrder(columnOrder);
  }, [setColumnOrder, columnOrder]);

  useUpdateEffect(() => {
    setSortBy(currentSortBy);
  }, [currentSortBy]);

  const openColumnPicker = () => {
    setIsColumnPickerVisible(true);
  };

  const closeColumnPicker = () => {
    setIsColumnPickerVisible(false);
  };

  const isColumnHeaderHovered = (id: string) => {
    return hoveredColumn === id;
  };

  const showOrderDetails = (id: Order['id']) => {
    setView({ type: OrderPanelViewType.OrderDetails, id });
  };

  const isRowActive = (id: Order['id']) => {
    if (!view) {
      return false;
    }

    return (
      (view.type === OrderPanelViewType.OrderDetails ||
        view.type === OrderPanelViewType.EditOrder) &&
      view.id === id
    );
  };

  return (
    <Styles.Container>
      <Header
        onSettingsClick={openColumnPicker}
        totalNumberOfOrders={totalNumberOfOrders}
      />
      {isLoading || isError ? null : (
        <>
          <Styles.OrdersTable {...getTableProps()}>
            <Styles.GridHeader bodyScrollbarWidth={bodyScrollbarWidth}>
              {headerGroups.map((headerGroup) => (
                // eslint-disable-next-line react/jsx-key
                <TableHeaderRow {...headerGroup.getHeaderGroupProps()}>
                  <Styles.RowSelectorHeaderCell />

                  {headerGroup.headers.map((column) => {
                    const shouldDisplaySortIcon =
                      column.isSorted || isColumnHeaderHovered(column.id);

                    return (
                      // eslint-disable-next-line react/jsx-key
                      <TableHeaderCell
                        {...column.getHeaderProps(
                          column.getSortByToggleProps()
                        )}
                        sorted={column.isSorted}
                        onMouseEnter={() => setHoveredColumn(column.id)}
                        onMouseLeave={() => setHoveredColumn('')}
                      >
                        <TableHeaderCellTitle title={String(column.Header)}>
                          {column.render('Header')}
                        </TableHeaderCellTitle>

                        {shouldDisplaySortIcon && (
                          <TableHeaderCellSortIcon>
                            {column.isSortedDesc && <ChevronDownIcon />}
                            {!column.isSortedDesc && <ChevronUpIcon />}
                          </TableHeaderCellSortIcon>
                        )}
                      </TableHeaderCell>
                    );
                  })}
                </TableHeaderRow>
              ))}
            </Styles.GridHeader>

            <Styles.GridContent ref={bodyRef}>
              {hasOrders && (
                <Styles.GridBody
                  {...getTableBodyProps({
                    style: { height: `${totalSize}px` },
                  })}
                >
                  {virtualItems.map((virtualRow) => {
                    const row = rows[virtualRow.index];
                    const order = row.original;
                    const isActive = isRowActive(order.id);

                    prepareRow(row);

                    return (
                      // eslint-disable-next-line react/jsx-key
                      <Styles.GridRow
                        {...row.getRowProps({
                          style: {
                            transform: `translateY(${virtualRow.start}px)`,
                          },
                        })}
                        ref={virtualRow.measureRef}
                        active={isActive}
                      >
                        <Styles.RowSelectorRowCell>
                          <RowSelector
                            checked={isActive}
                            onChange={() => showOrderDetails(order.id)}
                          />
                        </Styles.RowSelectorRowCell>

                        {row.cells.map((cell) => (
                          // eslint-disable-next-line react/jsx-key
                          <TableCell
                            {...cell.getCellProps()}
                            title={cell.value}
                            columnHeaderHovered={isColumnHeaderHovered(
                              cell.column.id
                            )}
                          >
                            <GridCellValue
                              column={cell.column.id as keyof Order}
                              order={row.original}
                              timeZone={timeZone}
                            />
                          </TableCell>
                        ))}
                      </Styles.GridRow>
                    );
                  })}
                </Styles.GridBody>
              )}

              {!hasOrders && <TableNoResults>No orders found</TableNoResults>}

              {isColumnPickerVisible && (
                <Styles.ColumnPickerWrapper>
                  <ColumnPicker
                    columns={selectedColumns}
                    onChange={setSelectedColumns}
                    onSave={(columns) => {
                      saveSelectedColumns(columns);
                      closeColumnPicker();
                    }}
                    onCancel={() => {
                      resetSelectedColumns();
                      closeColumnPicker();
                    }}
                  />
                </Styles.ColumnPickerWrapper>
              )}
            </Styles.GridContent>
          </Styles.OrdersTable>
        </>
      )}
    </Styles.Container>
  );
};
