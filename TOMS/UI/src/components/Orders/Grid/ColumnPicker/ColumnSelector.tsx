import { ChangeEventHandler } from 'react';

import { SelectableColumn } from '../types';
import * as Styles from './ColumnSelector.styles';

type Props = {
  columns: SelectableColumn[];
  title: string;
  onSelected(columns: SelectableColumn[]): void;
};

const areAllColumnsSelected = (columns: SelectableColumn[]) => {
  return columns.every((column) => column.isVisible);
};

export const ColumnSelector = (props: Props) => {
  const { columns, onSelected, title } = props;

  const toggleColumn = (selectedColumn: SelectableColumn) => {
    const updatedColumns = columns.map((column) => {
      return selectedColumn.id === column.id
        ? { ...column, isVisible: !column.isVisible }
        : column;
    });

    onSelected(updatedColumns);
  };

  const onSelectedAllChange: ChangeEventHandler<HTMLInputElement> = (event) => {
    const updatedColumns = columns.map((column) => ({
      ...column,
      isVisible: event.target.checked,
    }));

    onSelected(updatedColumns);
  };

  return (
    <Styles.Wrapper>
      <Styles.Header>
        <Styles.Title>{title}</Styles.Title>
        <Styles.SelectAllLabel
          control={
            <Styles.SelectAllCheckbox
              checked={areAllColumnsSelected(columns)}
              onChange={onSelectedAllChange}
            />
          }
          label="Select All"
        />
      </Styles.Header>
      <Styles.Columns>
        {columns.map((column) => (
          <Styles.Column key={String(column.id)}>
            <Styles.ColumnSelectButton
              selected={column.isVisible}
              onClick={() => toggleColumn(column)}
            >
              {column.title}
              {column.isVisible && <Styles.SelectedIcon />}
            </Styles.ColumnSelectButton>
          </Styles.Column>
        ))}
      </Styles.Columns>
    </Styles.Wrapper>
  );
};
