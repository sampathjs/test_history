package com.jm.shanghai.accounting.udsr.model.retrieval;

import com.jm.shanghai.accounting.udsr.model.fixed.ConfigurationItem;

/*
 * History:
 * 2018-11-17	V1.0		jwaechter		- Initial Version
 */

/**
 * Saves one row of the retrieval configuration table {@link ConfigurationItem#RETRIEVAL_CONFIG_TABLE_NAME}.
 * Semantically contains the raw retrieval logic and the columms aliases among the different tables 
 * (runtime / account mapping / tax code mapping / material code mapping / UDSR output table)
 * @author jwaechter
 * @version 1.0
 */
public class RetrievalConfiguration implements Comparable<RetrievalConfiguration> {
	private int priority;
	private String colNameRuntimeTable;
	private String colNameMappingTable;
	private String colNameReportOutput;
	private String colNameTaxTable;
	private String colNameMaterialNumberTable;
	private String retrievalLogic;
			
	public RetrievalConfiguration (int priority,
			String colNameRuntimeTable,
			String colNameMappingTable,
			String colNameReportOutput,
			String colNameTaxTable,
			String colNameMaterialNumberTable,
		    String retrievalLogic) {
		this.priority = priority;
		this.colNameMappingTable = colNameMappingTable;
		this.colNameReportOutput = colNameReportOutput;
		this.colNameRuntimeTable = colNameRuntimeTable;
		this.colNameTaxTable = colNameTaxTable;
		this.colNameMaterialNumberTable = colNameMaterialNumberTable;
		this.retrievalLogic = retrievalLogic;
	}

	public int getPriority() {
		return priority;
	}

	public void setPriority(int priority) {
		this.priority = priority;
	}

	public String getColNameRuntimeTable() {
		return colNameRuntimeTable;
	}

	public void setColNameRuntimeTable(String colNameRuntimeTable) {
		this.colNameRuntimeTable = colNameRuntimeTable;
	}

	public String getColNameMappingTable() {
		return colNameMappingTable;
	}

	public void setColNameMappingTable(String colNameMappingTable) {
		this.colNameMappingTable = colNameMappingTable;
	}

	public String getColNameReportOutput() {
		return colNameReportOutput;
	}

	public void setColNameReportOutput(String colNameReportOutput) {
		this.colNameReportOutput = colNameReportOutput;
	}

	public String getRetrievalLogic() {
		return retrievalLogic;
	}

	public void setRetrievalLogic(String retrievalLogic) {
		this.retrievalLogic = retrievalLogic;
	}
	
	public String getColNameTaxTable() {
		return colNameTaxTable;
	}

	public void setColNameTaxTable(String colNameTaxTable) {
		this.colNameTaxTable = colNameTaxTable;
	}

	public String getColNameMaterialNumberTable() {
		return colNameMaterialNumberTable;
	}

	public void setColNameMaterialNumberTable(String colNameMaterialNumberTable) {
		this.colNameMaterialNumberTable = colNameMaterialNumberTable;
	}

	@Override
	public String toString() {
		return "RetrievalConfiguration [priority=" + priority
				+ ", colNameRuntimeTable=" + colNameRuntimeTable
				+ ", colNameMappingTable=" + colNameMappingTable
				+ ", colNameReportOutput=" + colNameReportOutput
				+ ", colNameTaxTable=" + colNameTaxTable
				+ ", colNameMaterialNumberTable=" + colNameMaterialNumberTable
				+ ", retrievalLogic=" + retrievalLogic + "]";
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime
				* result
				+ ((colNameMappingTable == null) ? 0 : colNameMappingTable
						.hashCode());
		result = prime
				* result
				+ ((colNameMaterialNumberTable == null) ? 0
						: colNameMaterialNumberTable.hashCode());
		result = prime
				* result
				+ ((colNameReportOutput == null) ? 0 : colNameReportOutput
						.hashCode());
		result = prime
				* result
				+ ((colNameRuntimeTable == null) ? 0 : colNameRuntimeTable
						.hashCode());
		result = prime * result
				+ ((colNameTaxTable == null) ? 0 : colNameTaxTable.hashCode());
		result = prime * result + priority;
		result = prime * result
				+ ((retrievalLogic == null) ? 0 : retrievalLogic.hashCode());
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
		RetrievalConfiguration other = (RetrievalConfiguration) obj;
		if (colNameMappingTable == null) {
			if (other.colNameMappingTable != null)
				return false;
		} else if (!colNameMappingTable.equals(other.colNameMappingTable))
			return false;
		if (colNameMaterialNumberTable == null) {
			if (other.colNameMaterialNumberTable != null)
				return false;
		} else if (!colNameMaterialNumberTable
				.equals(other.colNameMaterialNumberTable))
			return false;
		if (colNameReportOutput == null) {
			if (other.colNameReportOutput != null)
				return false;
		} else if (!colNameReportOutput.equals(other.colNameReportOutput))
			return false;
		if (colNameRuntimeTable == null) {
			if (other.colNameRuntimeTable != null)
				return false;
		} else if (!colNameRuntimeTable.equals(other.colNameRuntimeTable))
			return false;
		if (colNameTaxTable == null) {
			if (other.colNameTaxTable != null)
				return false;
		} else if (!colNameTaxTable.equals(other.colNameTaxTable))
			return false;
		if (priority != other.priority)
			return false;
		if (retrievalLogic == null) {
			if (other.retrievalLogic != null)
				return false;
		} else if (!retrievalLogic.equals(other.retrievalLogic))
			return false;
		return true;
	}

	@Override
	public int compareTo(RetrievalConfiguration o) {
		int diff = this.priority-o.priority;
		if (diff == 0) {
			diff = this.colNameRuntimeTable.compareTo(o.colNameRuntimeTable);
		}
		return diff;
	}
}
