package com.jm.shanghai.accounting.udsr.model.retrieval;

import java.util.HashMap;
import java.util.Map;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;

/*
 * History:
 * 2018-11-17	V1.0		jwaechter		- Initial Version
 * 2019-07-16	V1.1		jwaechter		- Changing the class to a map
 */

/**
 * Saves one row of the retrieval configuration table {@link ConfigurationItem#RETRIEVAL_CONFIG_TABLE_NAME}.
 * Semantically contains the raw retrieval logic and the columms aliases among the different tables 
 * (runtime / account mapping / tax code mapping / material code mapping / UDSR output table)
 * @author jwaechter
 * @version 1.1
 */
public class RetrievalConfiguration implements Comparable<RetrievalConfiguration> {
	private Map<String, String> columnValues;		
	private final RetrievalConfigurationColDescriptionLoader colLoader;
	private int priority=-1;
	
	public RetrievalConfiguration (final RetrievalConfigurationColDescriptionLoader colLoader) {
		this.columnValues = new HashMap<>();
		this.colLoader = colLoader;
	}

	public String getColumnValue (RetrievalConfigurationColDescription retrievalConfigurationColDescription) {
		return columnValues.get(retrievalConfigurationColDescription.getColName());
	}

	public String getColumnValue (String colName) {
		return columnValues.get(colName);
	}
	
	public void setColumnValue (RetrievalConfigurationColDescription col, String value) {
		columnValues.put(col.getColName(), value);
	}
	
	public int getPriority() {
		return Integer.parseInt(columnValues.get(colLoader.getPriority().getColName()));
	}

	public void setPriority(int priority) {
		columnValues.put(colLoader.getPriority().getColName(), Integer.toString(priority));
		this.priority = priority;
	}

	

	@Override
	public String toString() {
		return "RetrievalConfiguration [columnValues=" + columnValues + "]";
	}

	@Override
	public int compareTo(RetrievalConfiguration o) {
		int diff = this.priority-o.priority;
		if (diff == 0) {
			String colNameMappingTable = this.columnValues.get(colLoader.getRuntimeDataTable());
			String colNameMappingTableOther = o.columnValues.get(colLoader.getRuntimeDataTable());
			if (colNameMappingTable != null && colNameMappingTableOther != null) {
				diff = colNameMappingTable.compareTo(colNameMappingTableOther);
			}
		}
		return diff;
	}
}
