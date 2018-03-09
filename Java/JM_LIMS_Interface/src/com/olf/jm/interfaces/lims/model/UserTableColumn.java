package com.olf.jm.interfaces.lims.model;

import com.olf.openjvs.enums.COL_TYPE_ENUM;


/*
 * History:
 * 2015-08-20 	V1.0	jwaechter	-	Initial version
 */

/**
 * Interface providing access to user table column metadata 
 * @author jwaechter
 * @version 1.0
 */
public interface UserTableColumn {
	public String getColName ();
	
	public COL_TYPE_ENUM getColType ();
	
	public String getColTitle ();
}
