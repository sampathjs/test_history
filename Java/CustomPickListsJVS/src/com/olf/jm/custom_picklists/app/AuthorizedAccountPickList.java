package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

/*
 * History:
 * 2016-08-30	V1.0	jwaechter	-	Initial Version
 */

/**
 * Class filtering a for authorised accounts
 * @author jwaechter
 * @version 1.0
 */
public class AuthorizedAccountPickList implements IScript {
	@Override
	public void execute(IContainerContext context) throws OException
	{
		String sql =
				"\nSELECT account_id "
			+	"\nFROM account"
			+	"\nWHERE account_status = 1"
				;
		Table retValues = context.getReturnTable();
	    boolean isVersionLessThan17 = retValues.getColName(1).equalsIgnoreCase("table_value");
	    if (isVersionLessThan17) {
	      retValues = retValues.getTable(("table_value"), 1);
	    }
	    
		Table sqlResult = Table.tableNew("sql_result");
		DBaseTable.execISql(sqlResult, sql);
		retValues.addCol("delete", COL_TYPE_ENUM.COL_INT);
		sqlResult.addCol("delete", COL_TYPE_ENUM.COL_INT);
		retValues.setColValInt("delete", 1);
		retValues.select(sqlResult, "delete", "account_id EQ $" + (isVersionLessThan17 ? "id" : "value"));
		retValues.deleteWhereValue("delete", 1);
		retValues.delCol("delete");
	}	
}
