package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;



public class B2BLegalEntityPickList implements IScript {
	@Override
	public void execute(IContainerContext context) throws OException
	{
		  
		String sql =
				"\nSELECT * "
			+	"\nFROM Party" 
			+	"\nWHERE party_class = 0 and int_ext = 0" ;
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
			retValues.select(sqlResult, "delete", "party_id EQ $" + (isVersionLessThan17 ? "id" : "value"));
			 retValues.deleteWhereValue("delete", 1);
			retValues.delCol("delete"); 
	}	
}
