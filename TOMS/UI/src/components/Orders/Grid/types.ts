import { Column as TableColumn } from 'react-table';

import { Order } from 'types';

export type Column = TableColumn<Order> &
  Required<Pick<TableColumn<Order>, 'accessor' | 'Header'>>;

export type SelectableColumn = {
  id: Column['accessor'];
  title: Column['Header'];
  isVisible: boolean;
};
