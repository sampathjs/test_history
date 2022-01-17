import compact from 'lodash/compact';
import isEmpty from 'lodash/isEmpty';
import isEqual from 'lodash/isEqual';
import { useEffect, useState } from 'react';

import { Button } from 'components/Button';

import { SelectableColumn } from '../types';
import * as Styles from './ColumnPicker.styles';
import { ColumnSelector } from './ColumnSelector';
import { ColumnSorter } from './ColumnSorter';

type ColumnId = SelectableColumn['id'];

const defaultColumns: ColumnId[] = [
  'id',
  'createdAt',
  'displayStringOrderStatus',
  'displayStringBuySell',
  'displayStringOrderType',
  'fillPercentage',
  'displayStringInternalBu',
  'displayStringTicker',
  'displayStringExternalBu',
  'displayStringBaseQuantityUnit',
  'reference',
];

const otherColumns: ColumnId[] = [
  'baseQuantity',
  'displayStringContractType',
  'displayStringMetalForm',
  'displayStringMetalLocation',
  'version',
];

type Props = {
  columns: SelectableColumn[];
  onChange(columns: SelectableColumn[]): void;
  onSave(columns: SelectableColumn[]): void;
  onCancel(): void;
};

const getColumnsByIds = (ids: ColumnId[], columns: SelectableColumn[]) => {
  const foundColumns = ids.map((id) =>
    columns.find((column) => column.id === id)
  );

  return compact(foundColumns);
};

const getVisibleColumns = (columns: SelectableColumn[]) => {
  return columns.filter((column) => column.isVisible);
};

export const ColumnPicker = (props: Props) => {
  const { onCancel, onChange, onSave } = props;
  const [initialColumns] = useState(props.columns);
  const [columns, setColumns] = useState(initialColumns);
  const hasVisibleColumns = !isEmpty(getVisibleColumns(columns));
  const hasAnyChanges =
    !isEqual(getVisibleColumns(initialColumns), getVisibleColumns(columns)) &&
    hasVisibleColumns;

  useEffect(() => {
    onChange(columns);
  }, [columns, onChange]);

  const onColumnsSelected = (selectedColumns: SelectableColumn[]) => {
    setColumns((columns) => {
      return columns.map((column) => {
        const selectedColumn = selectedColumns.find(
          (selectedColumn) => selectedColumn.id === column.id
        );

        return selectedColumn ?? column;
      });
    });
  };

  const onColumnsSort = (sortedColumns: SelectableColumn[]) => {
    setColumns(sortedColumns);
  };

  return (
    <Styles.Wrapper>
      <Styles.Header>
        <Styles.Title>Column Configuration</Styles.Title>
      </Styles.Header>
      <ColumnSorter columns={columns} onSort={onColumnsSort} />
      <Styles.ColumnSelectors>
        <Styles.ColumnSelector>
          <ColumnSelector
            title="Default"
            columns={getColumnsByIds(defaultColumns, columns)}
            onSelected={onColumnsSelected}
          />
        </Styles.ColumnSelector>
        <Styles.ColumnSelector>
          <ColumnSelector
            title="Other"
            columns={getColumnsByIds(otherColumns, columns)}
            onSelected={onColumnsSelected}
          />
        </Styles.ColumnSelector>
      </Styles.ColumnSelectors>
      <Styles.Actions>
        <Button variant="outlined" color="secondary" onClick={onCancel}>
          Cancel
        </Button>
        <Button onClick={() => onSave(columns)} disabled={!hasAnyChanges}>
          Save
        </Button>
      </Styles.Actions>
    </Styles.Wrapper>
  );
};
