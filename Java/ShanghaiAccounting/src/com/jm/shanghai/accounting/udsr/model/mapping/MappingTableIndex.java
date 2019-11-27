package com.jm.shanghai.accounting.udsr.model.mapping;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationTableCols;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2018-12-28	V1.0	jwaechter	- Initial Version
 */

/**
 * Contains {@link ColumnIndex} for each indexable mapping column of the mapping table.
 * @author jwaechter
 * @version 1.0
 */
public class MappingTableIndex {
	private final List<ColumnIndex> columnIndexes;
	private final Map<String, RetrievalConfiguration> retrievalConfigByMappingColName;
	private final RetrievalConfigurationColDescriptionLoader colLoader;
	
	public MappingTableIndex (final List<ColumnIndex> columnIndexes, 
			final Map<String, RetrievalConfiguration> retrievalConfigByMappingColName,
			final RetrievalConfigurationColDescriptionLoader colLoader) {
		this.columnIndexes = new ArrayList<>(columnIndexes);
		this.retrievalConfigByMappingColName = retrievalConfigByMappingColName;
		this.colLoader = colLoader;
	}
	
	/**
	 * Retrieves all possible MappingTableRows that match to the input based
	 * on indexable columns.
	 * @param runtimeTable
	 * @param rowId
	 * @return
	 */
	public Set<MappingTableRowConfiguration> filter (Table runtimeTable, int rowId) {
		Set<MappingTableRowConfiguration> matchingRows = null;
		for (ColumnIndex colIndex : columnIndexes) {
			RetrievalConfiguration rc = retrievalConfigByMappingColName.get(colIndex.getColumn().getColName());
			if (rc == null) {
				continue;
			}
			
			String colValue = runtimeTable.getDisplayString(runtimeTable.getColumnId(rc.getColumnValue(colLoader.getRuntimeDataTable().getColName())), rowId);
			if (colValue == null) {
				colValue = "";
			}
			Set<MappingTableRowConfiguration> matchingRowsForColumn = colIndex.getKleeneStarRows();
			Set<MappingTableRowConfiguration> alternatives = colIndex.getAlternativesMappingTableRows(colValue);
			
			matchingRowsForColumn.addAll(alternatives);				
			
			colIndex.getAlternativesMappingTableRows(colValue);
			if (matchingRows == null) {
				matchingRows = new HashSet<>(matchingRowsForColumn);
			} else {
				matchingRows.retainAll(matchingRowsForColumn);
			}
			if (matchingRows.size() == 0) {
				return matchingRows;
			}
		}
		if (matchingRows == null) {
			matchingRows = new HashSet<>();
		}
		return matchingRows;
	}
}
