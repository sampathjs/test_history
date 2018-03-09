package com.olf.jm.stocktake.dataTables;

import com.olf.openrisk.table.EnumColType;

/**
 * The Interface ITableColumns. Represents a column in a user table defining the column name 
 * and the column type.
 */
public interface ITableColumn {
	/**
	 * Gets the column name.
	 *
	 * @return the column name
	 */
	String getColumnName();
	
	/**
	 * Gets the column type.
	 *
	 * @return the column type
	 */
	EnumColType getColumnType();
	
}
