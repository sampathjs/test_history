import { SortingRule } from 'react-table';

import { OrderSortAttribute } from 'hooks/useOrders';
import { Order, SortDirection } from 'types';

import { Column } from './types';

export const columns: Column[] = [
  {
    Header: 'ID',
    accessor: 'id',
    width: 65,
  },
  {
    Header: 'Executed At',
    accessor: 'createdAt',
  },
  {
    Header: 'Status',
    accessor: 'displayStringOrderStatus',
    width: 70,
  },
  {
    Header: 'B/S',
    accessor: 'displayStringBuySell',
    width: 60,
  },
  {
    Header: 'Type',
    accessor: 'displayStringOrderType',
    width: 50,
  },
  {
    Header: 'Fill',
    accessor: 'fillPercentage',
    width: 90,
  },
  {
    Header: 'JM PMM Unit',
    accessor: 'displayStringInternalBu',
    width: 120,
  },
  {
    Header: 'Ticker',
    accessor: 'displayStringTicker',
    width: 80,
  },
  {
    Header: 'Counterparty',
    accessor: 'displayStringExternalBu',
    width: 120,
  },
  {
    Header: 'Unit',
    accessor: 'displayStringBaseQuantityUnit',
    width: 50,
  },
  {
    Header: 'Quantity',
    accessor: 'baseQuantity',
    width: 70,
  },
  {
    Header: 'Contract Type',
    accessor: 'displayStringContractType',
    width: 70,
  },
  {
    Header: 'Form',
    accessor: 'displayStringMetalForm',
    width: 50,
  },
  {
    Header: 'Location',
    accessor: 'displayStringMetalLocation',
    width: 50,
  },
  {
    Header: 'Ver.',
    accessor: 'version',
    width: 40,
  },
  {
    Header: 'Ref',
    accessor: 'reference',
    width: 40,
  },
];

const columnSortMappings: Partial<
  Record<keyof Order, OrderSortAttribute['id']>
> = {
  displayStringOrderStatus: 'idOrderStatus',
  displayStringBuySell: 'idBuySell',
  displayStringInternalBu: 'idInternalBu',
  displayStringTicker: 'idTicker',
  displayStringExternalBu: 'idExternalBu',
  displayStringBaseQuantityUnit: 'idBaseQuantityUnit',
  displayStringContractType: 'idContractType',
  displayStringMetalForm: 'idMetalForm',
  displayStringMetalLocation: 'idMetalLocation',
  displayStringOrderType: 'idOrderType',
};

export const mapSortingRulesToSortAttributes = (
  rules: SortingRule<Order>[]
) => {
  return rules.map<OrderSortAttribute>((rule) => {
    const id = rule.id as keyof Order;

    return {
      id: columnSortMappings[id] ?? id,
      direction: rule.desc ? SortDirection.desc : SortDirection.asc,
    };
  });
};
