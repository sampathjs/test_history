package com.olf.jm.blockbackdateddealentry.model;

import com.olf.openrisk.table.EnumColType;

/*
 * History: 
 * 2015-07-02 	V1.0	jwaechter	- Initial version
 */

/**
 * Interface used to represent column meta data. It's intended to be implemented by 
 * enums to allow abstraction from concrete enum types representing the columns of certain user tables.
 * @author jwaechter
 * @version 1.0
 */
public interface UserTableCols {
	public String getColName();

	public String getColTitle();

	public EnumColType getColType();
}
