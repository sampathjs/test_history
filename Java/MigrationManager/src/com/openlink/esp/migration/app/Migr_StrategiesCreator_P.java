// ==================================================================================
// Project:     Migration Manager / Strategies
// Script Name: Migr_StrategiesCreator_P
// Script Type: Param
// 
// Revision History:
// 1.0 - 15.10.2012 - roberbec - initial version
// 
// Description: Sets the strategies user table name.
// 
//  !!!  OBSOLETE - will be removed in future
// ==================================================================================

package com.openlink.esp.migration.app;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

public class Migr_StrategiesCreator_P implements IScript
{
	String USER_TABLE_NAME = "user_migr_strategies";

	public void execute(IContainerContext context) throws OException
	{
		Table argt = context.getArgumentsTable();
	//	Table returnt = context.getReturnTable();

		int row;
		String col = "user_table";

		argt.addCol(col, COL_TYPE_ENUM.COL_STRING);
		row = argt.addRow();
		argt.setString(col, row, USER_TABLE_NAME);
	}
}
