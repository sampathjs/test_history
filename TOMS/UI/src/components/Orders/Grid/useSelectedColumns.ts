import { useEffect, useState } from 'react';
import useLocalStorage from 'react-use/lib/useLocalStorage';

import { columns } from './helpers';
import { SelectableColumn } from './types';

const allColumns: SelectableColumn[] = columns.map((column) => ({
  id: column.accessor,
  title: column.Header,
  isVisible: true,
}));

export const useSelectedColumns = () => {
  const [storedColumns = allColumns, setStoredColumns] = useLocalStorage(
    'orders.columns',
    allColumns
  );
  const [selectedColumns, setSelectedColumns] = useState(storedColumns);

  useEffect(() => {
    setSelectedColumns(storedColumns);
  }, [storedColumns]);

  const resetSelectedColumns = () => {
    setSelectedColumns(storedColumns);
  };

  return {
    selectedColumns,
    setSelectedColumns,
    saveSelectedColumns: setStoredColumns,
    resetSelectedColumns,
  };
};
