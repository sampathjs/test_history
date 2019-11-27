package com.olf.jm.custom_picklists.app;

import com.olf.openjvs.*;
import com.olf.openjvs.enums.*;

/*
 * History:
 * 2015-11-03	V1.0	jwaechter	-	Initial Version
 */

/**
 *
 * @author ifernandes
 * @version 1.0
 */
public class JMBasePriceRefSrcPickList implements IScript {
	@Override
	public void execute(IContainerContext context) throws OException
	{
		//String sql ="SELECT id_number, name FROM currency WHERE precious_metal = 0";
		Table retValues = context.getReturnTable().getTable("table_value", 1);
		//Table sqlResult = Table.tableNew("sql_result");
		//DBaseTable.execISql(sqlResult, sql);
		//retValues.addCol("delete", COL_TYPE_ENUM.COL_INT);
		//sqlResult.addCol("delete", COL_TYPE_ENUM.COL_INT);
		//retValues.setColValInt("delete", 1);
		//retValues.select(sqlResult, "delete", "id_number EQ $id");
		//retValues.deleteWhereValue("delete", 1);
		//retValues.delCol("delete");
		
		for(int i = retValues.getNumRows();i>0;i--){
			
			String strValue = retValues.getString("label", i);
			                          
			if( (Str.equal(strValue, "JM London Opening") != 1)
			  && (Str.equal(strValue, "JM HK Opening") != 1)
			  && (Str.equal(strValue, "JM HK Closing") !=1)
			  && (Str.equal(strValue, "JM NY Opening") != 1)
			  && (Str.equal(strValue, "LME AM") != 1)
  			  && (Str.equal(strValue, "LME PM") != 1)
  			  && (Str.equal(strValue, "LBMA AM") != 1)
			  && (Str.equal(strValue, "LBMA Silver") != 1)
			  && (Str.equal(strValue, "LBMA PM") != 1)
					
					){
				retValues.delRow(i);
			}

			
		}
		
		
	}	
}
