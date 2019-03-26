package com.jm.shanghai.accounting.udsr.model.retrieval;

import com.olf.openrisk.table.EnumColType;
import com.olf.openrisk.table.TableColumn;

/*
 * History:
 * 2018-11-27	V1.0		jwaechter	- Initial Version
 */

/**
 * Contains data for a column of the java table.
 * @author jwaechter
 * @version 1.0
 */
public class JavaTableColumn {
	private final EnumColType colType;
	private String colName;
	private final int colId;
	private final Object defaultValue;
	
	public JavaTableColumn (final String colName,
			final EnumColType colType,
			final Object defaultValue,
			final int colId) {
		this.colName = colName;
		this.colType = colType;
		this.colId = colId;
		this.defaultValue = defaultValue;
	}
	
	public JavaTableColumn (TableColumn endurTableColumn, 
			final Object defaultValue,
			final int colId) {
		this(endurTableColumn.getName(), endurTableColumn.getType(), defaultValue, colId);
	}
		
	public JavaTableColumn (TableColumn endurTableColumn, 
			final Object defaultValue) {
		this(endurTableColumn, defaultValue, endurTableColumn.getNumber());
	}

	public EnumColType getColType() {
		return colType;
	}

	public String getColName() {
		return colName;
	}
	
	public void setColName(String newColName) {
		// TODO Auto-generated method stub
		this.colName = newColName;
	}

	public int getColId() {
		return colId;
	}

	public Object getDefaultValue() {
		return defaultValue;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + colId;
		result = prime * result + ((colType == null) ? 0 : colType.hashCode());
		result = prime * result
				+ ((defaultValue == null) ? 0 : defaultValue.hashCode());
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
		JavaTableColumn other = (JavaTableColumn) obj;
		if (colId != other.colId)
			return false;
		if (colType != other.colType)
			return false;
		if (defaultValue == null) {
			if (other.defaultValue != null)
				return false;
		} else if (!defaultValue.equals(other.defaultValue))
			return false;
		return true;
	}

	
	@Override
	public String toString() {
		return "JavaTableColumn [colType=" + colType + ", colName=" + colName
				+ ", colId=" + colId + ", defaultValue=" + defaultValue + "]";
	}
	
}
