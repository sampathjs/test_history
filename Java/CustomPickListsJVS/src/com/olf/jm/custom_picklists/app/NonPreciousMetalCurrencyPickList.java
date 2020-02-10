package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

/*
 * History:
 * 2015-11-03	V1.0	jwaechter	-	Initial Version
 */

/**
 * Class filtering a currency list to contain non precious metal currencies only.
 * @author jwaechter
 * @version 1.0
 */
public class NonPreciousMetalCurrencyPickList implements IScript {
	@Override
	public void execute(IContainerContext context) throws OException
	{
		String sql =
				"SELECT id_number, name FROM currency WHERE precious_metal = 0";
		
		Table retValues = context.getReturnTable();
		
		/*** v17 change - Structure of return table has changed. Added check below. ***/
		boolean isV14 = retValues.getColName(1).equalsIgnoreCase("table_value");
		
		if (isV14) {
			retValues = retValues.getTable("table_value", 1);
		} 		
		
		Table sqlResult = Table.tableNew("sql_result");
		DBaseTable.execISql(sqlResult, sql);
		retValues.addCol("delete", COL_TYPE_ENUM.COL_INT);
		sqlResult.addCol("delete", COL_TYPE_ENUM.COL_INT);
		retValues.setColValInt("delete", 1);
		
		/*** v17 change - Use column name value. For v14 continue using id_number ***/
		retValues.select(sqlResult, "delete", "id_number EQ $" + (isV14 ? "id" : "value"));

		retValues.deleteWhereValue("delete", 1);
		retValues.delCol("delete");
	}	
}
