package com.olf.jm.SapInterface.businessObjects.enums;

import com.olf.openrisk.table.EnumColType;

/**
 * The Interface ITableColumns.
 */
public interface ITableColumn {
	/**
	 * Gets the column name.
	 *
	 * @return the column name
	 */
	String getColumnName();
	
	/**
	 * Gets the column name adding the supplied table space, Assumes the
	 * column names are defiend in the Enum with no name space..
	 *
	 * @param nameSpace the name space to apply to the column name.
	 * @return the column name
	 */
	String getColumnName(String nameSpace);
	
	/**
	 * Gets the column type.
	 *
	 * @return the column type
	 */
	EnumColType getColumnType();
	
	/**
	 *  
	 * Flag to indicate the the field is a required field.
	 *
	 * @return true, if is required field
	 */
	boolean isRequiredField();
}
