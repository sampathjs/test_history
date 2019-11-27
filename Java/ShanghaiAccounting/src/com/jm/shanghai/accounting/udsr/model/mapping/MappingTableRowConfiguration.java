package com.jm.shanghai.accounting.udsr.model.mapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.ColNameProvider;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfiguration;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationColDescriptionLoader;
import com.jm.shanghai.accounting.udsr.model.retrieval.RetrievalConfigurationTableCols;
import com.olf.openrisk.table.Table;

/*
 * History:
 * 2018-11-22		V1.0		jwaechter		- Initial Version
 */

/**
 * Contains the data of a row in the mapping table.
 * @author jwaechter
 * @version 1.0
 */
public class MappingTableRowConfiguration implements Comparable<MappingTableRowConfiguration>{
	/**
	 * The artificially created unique row id.
	 */
	private final int uniqueRowId;
	
	private final List<MappingTableCellConfiguration> cellConfigurations;
	
	private final Map<String, RetrievalConfiguration> mappingColNamesToRetrievalConfig;

	private final RetrievalConfigurationColDescriptionLoader colLoader;
	
	public MappingTableRowConfiguration (
			ColNameProvider colNameProvider,
			final int uniqueRowId,
			final List<MappingTableCellConfiguration> cellConfigurations,
			final List<RetrievalConfiguration> retrievalConfig,
			final RetrievalConfigurationColDescriptionLoader colLoader) {
		this.uniqueRowId = uniqueRowId;
		this.cellConfigurations = new ArrayList<>(cellConfigurations);
		this.mappingColNamesToRetrievalConfig = new HashMap<String, RetrievalConfiguration>();
		this.colLoader = colLoader;
		for (RetrievalConfiguration rc : retrievalConfig) {
			mappingColNamesToRetrievalConfig.put(colNameProvider.getColName(rc), rc);
		}
	}

	public int getUniqueRowId() {
		return uniqueRowId;
	}

	public List<MappingTableCellConfiguration> getCellConfigurations() {
		return cellConfigurations;
	}
	
	/**
	 * Checks if a given runtime table row identified by it's unique row id 
	 * matches this mapping table row and retrieves the weight of the match. 
	 * The returned value is zero or positive in case of a match and negative in case
	 * the row does not match the runtimeTable row
	 * @param runtimeTable
	 * @param runtimeTableRowId
	 * @return 
	 */
	public int match (Table runtimeTable, int runtimeTableRowId) {
		int weight=0;
		for (MappingTableCellConfiguration cellConfig : cellConfigurations) {
			RetrievalConfiguration rc = 
					mappingColNamesToRetrievalConfig.get(cellConfig.getColConfig().getColName());
			String colNameRumtimeTable = rc.getColumnValue(colLoader.getRuntimeDataTable());
			int colId = runtimeTable.getColumnId(colNameRumtimeTable);
			String cellValue = runtimeTable.getDisplayString(colId, runtimeTableRowId);
			if (cellConfig.getPredicate().evaluate(cellValue)) {
				weight += cellConfig.getPredicate().getWeight();
			} else {
				return -1;
			}
		}
		return weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + uniqueRowId;
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MappingTableRowConfiguration other = (MappingTableRowConfiguration) obj;
		if (uniqueRowId != other.uniqueRowId)
			return false;
		return true;
	}

	@Override
	public int compareTo(MappingTableRowConfiguration o) {
		return o.uniqueRowId - uniqueRowId;
	}

	@Override
	public String toString() {
		return "MappingTableRowConfiguration [uniqueRowId=" + uniqueRowId + "]";
	}
}
