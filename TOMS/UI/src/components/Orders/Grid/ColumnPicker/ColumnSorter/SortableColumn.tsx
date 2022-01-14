import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';

import { SelectableColumn } from '../../types';
import * as Styles from './SortableColumn.styles';

type Props = {
  column: SelectableColumn;
};

export const SortableColumn = (props: Props) => {
  const { column } = props;
  const {
    attributes,
    isDragging,
    listeners,
    setNodeRef,
    transform,
    transition,
  } = useSortable({ id: String(column.id) });

  const style = {
    transform: CSS.Translate.toString(transform),
    transition,
  };

  return (
    <Styles.Column
      dragging={isDragging}
      ref={setNodeRef}
      style={style}
      {...attributes}
      {...listeners}
    >
      {column.title}
    </Styles.Column>
  );
};
