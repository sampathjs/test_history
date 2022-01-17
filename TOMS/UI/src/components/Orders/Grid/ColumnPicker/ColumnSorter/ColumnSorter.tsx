import { DndContext, DragEndEvent } from '@dnd-kit/core';
import { restrictToHorizontalAxis } from '@dnd-kit/modifiers';
import {
  arrayMove,
  horizontalListSortingStrategy,
  SortableContext,
} from '@dnd-kit/sortable';

import { SelectableColumn } from '../../types';
import * as Styles from './ColumnSorter.styles';
import { SortableColumn } from './SortableColumn';

type Props = {
  columns: SelectableColumn[];
  onSort(columns: SelectableColumn[]): void;
};

export const ColumnSorter = (props: Props) => {
  const { columns, onSort } = props;
  const columnIds = columns.map((column) => String(column.id));
  const visibleColumns = columns.filter((column) => column.isVisible);

  const onDragEnd = (event: DragEndEvent) => {
    const { active, over } = event;

    if (!over || active.id === over.id) {
      return;
    }

    const oldIndex = columns.findIndex((column) => column.id === active.id);
    const newIndex = columns.findIndex((column) => column.id === over.id);

    const sortedColumns = arrayMove(columns, oldIndex, newIndex);

    onSort(sortedColumns);
  };

  return (
    <DndContext modifiers={[restrictToHorizontalAxis]} onDragEnd={onDragEnd}>
      <Styles.Columns>
        <SortableContext
          items={columnIds}
          strategy={horizontalListSortingStrategy}
        >
          {visibleColumns.map((column) => (
            <SortableColumn key={String(column.id)} column={column} />
          ))}
        </SortableContext>
      </Styles.Columns>
    </DndContext>
  );
};
