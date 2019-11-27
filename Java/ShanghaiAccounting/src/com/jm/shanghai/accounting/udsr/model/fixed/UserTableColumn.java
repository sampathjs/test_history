package com.jm.shanghai.accounting.udsr.model.fixed;

import com.olf.openrisk.table.EnumColType;

/*
 * History:
 * 2015-08-20 	V1.0	jwaechter	-	Initial version
 */

/**
 * Interface providing access to user table column metadata 
 * @author jwaechter
 * @version 1.0
 * @deprecated  no longer used.
 */
public interface UserTableColumn {
	public String getColName ();
	
	public EnumColType getColType ();

	public String getColTypeName ();

	public String getColTitle ();
}
